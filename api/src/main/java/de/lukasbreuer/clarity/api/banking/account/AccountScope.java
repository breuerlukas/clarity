package de.lukasbreuer.clarity.api.banking.account;

import de.lukasbreuer.clarity.api.banking.connection.BankConnection;
import de.lukasbreuer.clarity.api.banking.connection.BankConnectionRepository;
import de.lukasbreuer.clarity.api.space.Space;
import de.lukasbreuer.clarity.api.space.SpaceKind;
import de.lukasbreuer.clarity.api.space.SpaceRepository;
import de.lukasbreuer.clarity.api.transaction.cash.CashWallet;
import de.lukasbreuer.clarity.api.transaction.cash.CashWalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Keeps the spaces apart: every view asks for the accounts of one space, or
 * of every space of one kind. The cash wallet counts as one of them, so
 * cash entries show up wherever bank transactions do.
 */
@Component
@RequiredArgsConstructor
public final class AccountScope {
  private final SpaceRepository spaceRepository;
  private final BankConnectionRepository connectionRepository;
  private final BankAccountRepository accountRepository;
  private final CashWalletRepository walletRepository;

  public CompletableFuture<Set<UUID>> accountIds(UUID spaceId) {
    return accountIds(Collections.singleton(spaceId));
  }

  public CompletableFuture<Set<UUID>> accountIds(SpaceKind kind) {
    return spaceRepository.findByKind(kind)
      .thenApply(spaces -> spaces.stream().map(Space::id)
        .collect(Collectors.toSet()))
      .thenCompose(this::accountIds);
  }

  private CompletableFuture<Set<UUID>> accountIds(Set<UUID> spaceIds) {
    return accountIds(connection -> spaceIds.contains(connection.spaceId()))
      .thenCombine(walletIds(spaceIds), this::union);
  }

  private CompletableFuture<Set<UUID>> walletIds(Set<UUID> spaceIds) {
    return walletRepository.findAll()
      .thenApply(wallets -> wallets.stream()
        .filter(wallet -> spaceIds.contains(wallet.spaceId()))
        .map(CashWallet::id)
        .collect(Collectors.toSet()));
  }

  private Set<UUID> union(Set<UUID> bankAccountIds, Set<UUID> walletIds) {
    var accountIds = new HashSet<>(bankAccountIds);
    accountIds.addAll(walletIds);
    return accountIds;
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
