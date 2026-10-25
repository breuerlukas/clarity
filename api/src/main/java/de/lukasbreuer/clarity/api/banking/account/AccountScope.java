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
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Keeps the spaces apart: every view asks for the accounts of one space, or
 * of every space of one kind. An account can be moved away from the space
 * of its connection, and hidden accounts belong to none for the time being.
 * The cash wallet counts as one of them, so cash entries show up wherever
 * bank transactions do.
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
    return connectionRepository.findAll()
      .thenCombine(accountRepository.findAll(), (connections, accounts) ->
        accountsIn(spaceIds, connections, accounts))
      .thenCombine(walletIds(spaceIds), this::union);
  }

  /**
   * An account belongs to the space it was moved to, else to the space of
   * its connection.
   */
  private Set<UUID> accountsIn(
    Set<UUID> spaceIds, List<BankConnection> connections,
    List<BankAccount> accounts
  ) {
    var byId = connections.stream().collect(Collectors.toMap(
      BankConnection::id, Function.identity()));
    return accounts.stream()
      .filter(account -> !account.isHidden())
      .filter(account -> byId.containsKey(account.connectionId()))
      .filter(account -> spaceIds.contains(
        account.spaceIn(byId.get(account.connectionId()))))
      .map(BankAccount::id)
      .collect(Collectors.toSet());
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
}