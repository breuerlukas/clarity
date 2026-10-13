package de.lukasbreuer.clarity.api.banking.account;

import de.lukasbreuer.clarity.api.banking.connection.BankConnection;
import de.lukasbreuer.clarity.api.banking.connection.BankConnectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Keeps the owner's personal money and the business apart: every view asks
 * for the accounts of one side only.
 */
@Component
@RequiredArgsConstructor
public final class AccountScope {
  private final BankConnectionRepository connectionRepository;
  private final BankAccountRepository accountRepository;

  public CompletableFuture<List<BankAccount>> accounts(boolean business) {
    return connectionRepository.findAll()
      .thenApply(connections -> connectionIds(connections, business))
      .thenCompose(connectionIds -> accountRepository.findAll()
        .thenApply(accounts -> accounts.stream()
          .filter(account -> connectionIds.contains(account.connectionId()))
          .toList()));
  }

  public CompletableFuture<Set<UUID>> accountIds(boolean business) {
    return accounts(business).thenApply(accounts -> accounts.stream()
      .map(BankAccount::id).collect(Collectors.toSet()));
  }

  private Set<UUID> connectionIds(
    List<BankConnection> connections, boolean business
  ) {
    return connections.stream()
      .filter(connection -> connection.isBusiness() == business)
      .map(BankConnection::id).collect(Collectors.toSet());
  }
}
