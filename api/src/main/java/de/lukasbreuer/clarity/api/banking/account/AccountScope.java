package de.lukasbreuer.clarity.api.banking.account;

import de.lukasbreuer.clarity.api.banking.connection.BankConnection;
import de.lukasbreuer.clarity.api.banking.connection.BankConnectionRepository;
import de.lukasbreuer.clarity.api.space.Space;
import de.lukasbreuer.clarity.api.space.SpaceKind;
import de.lukasbreuer.clarity.api.space.SpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Keeps the spaces apart: every view asks for the accounts of one space, or
 * of every space of one kind.
 */
@Component
@RequiredArgsConstructor
public final class AccountScope {
  private final SpaceRepository spaceRepository;
  private final BankConnectionRepository connectionRepository;
  private final BankAccountRepository accountRepository;

  public CompletableFuture<Set<UUID>> accountIds(UUID spaceId) {
    return accountIds(connection -> spaceId.equals(connection.spaceId()));
  }

  public CompletableFuture<Set<UUID>> accountIds(SpaceKind kind) {
    return spaceRepository.findByKind(kind)
      .thenApply(spaces -> spaces.stream().map(Space::id)
        .collect(Collectors.toSet()))
      .thenCompose(spaceIds -> accountIds(connection ->
        spaceIds.contains(connection.spaceId())));
  }

  private CompletableFuture<Set<UUID>> accountIds(
    Predicate<BankConnection> filter
  ) {
    return connectionRepository.findAll()
      .thenApply(connections -> connectionIds(connections, filter))
      .thenCompose(connectionIds -> accountRepository.findAll()
        .thenApply(accounts -> accounts.stream()
          .filter(account -> connectionIds.contains(account.connectionId()))
          .map(BankAccount::id)
          .collect(Collectors.toSet())));
  }

  private Set<UUID> connectionIds(
    List<BankConnection> connections, Predicate<BankConnection> filter
  ) {
    return connections.stream().filter(filter)
      .map(BankConnection::id).collect(Collectors.toSet());
  }
}
