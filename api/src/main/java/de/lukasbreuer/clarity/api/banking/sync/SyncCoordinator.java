package de.lukasbreuer.clarity.api.banking.sync;

import de.lukasbreuer.clarity.api.banking.EnableBankingSigner;
import de.lukasbreuer.clarity.api.banking.account.BankAccount;
import de.lukasbreuer.clarity.api.banking.account.BankAccountRepository;
import de.lukasbreuer.clarity.api.banking.connection.BankConnection;
import de.lukasbreuer.clarity.api.banking.connection.BankConnectionRepository;
import de.lukasbreuer.clarity.api.business.invoice.matching.InvoiceMatching;
import de.lukasbreuer.clarity.api.transaction.category.Categorizer;
import de.lukasbreuer.clarity.api.transaction.category.CategorizerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Decides which accounts to sync and makes sure only one sync runs at a time.
 * The hourly schedule only picks accounts that are due; a sync the owner
 * starts takes every account and tells the bank the owner is present, which
 * PSD2 does not count against the daily limit. New bookings are matched
 * against open invoices afterwards.
 */
@Component
@RequiredArgsConstructor
public final class SyncCoordinator {
  private final AccountSynchronizer accountSynchronizer;
  private final BankAccountRepository accountRepository;
  private final BankConnectionRepository connectionRepository;
  private final CategorizerFactory categorizerFactory;
  private final EnableBankingSigner signer;
  private final InvoiceMatching invoiceMatching;
  private final AtomicBoolean running = new AtomicBoolean(false);

  @Scheduled(fixedDelay = 1, initialDelay = 1, timeUnit = TimeUnit.HOURS)
  void synchronizeDueAccounts() {
    if (signer.isConfigured()) {
      synchronize(BankAccount::isDueForSync, Map.of()).join();
    }
  }

  /**
   * @param ownerHeaders Psu-Ip-Address and Psu-User-Agent of the owner
   */
  public CompletableFuture<Boolean> synchronizeNow(
    Map<String, String> ownerHeaders
  ) {
    return synchronize(_ -> true, ownerHeaders);
  }

  private CompletableFuture<Boolean> synchronize(
    Predicate<BankAccount> filter, Map<String, String> headers
  ) {
    if (!running.compareAndSet(false, true)) {
      return CompletableFuture.completedFuture(false);
    }
    return connectionRepository.findAll()
      .thenCompose(connections -> categorizerFactory.create()
        .thenCompose(categorizer -> synchronize(validConnectionIds(connections),
          categorizer, filter, headers)))
      .thenRun(invoiceMatching::matchAll)
      .handle((_, _) -> finish());
  }

  private boolean finish() {
    running.set(false);
    return true;
  }

  private CompletableFuture<Void> synchronize(
    Set<UUID> connectionIds, Categorizer categorizer,
    Predicate<BankAccount> filter, Map<String, String> headers
  ) {
    return accountRepository.findAll()
      .thenCompose(accounts -> synchronize(accounts.stream()
        .filter(account -> connectionIds.contains(account.connectionId()))
        .filter(filter).toList(), categorizer, headers));
  }

  private CompletableFuture<Void> synchronize(
    List<BankAccount> accounts, Categorizer categorizer,
    Map<String, String> headers
  ) {
    return CompletableFuture.allOf(accounts.stream()
      .map(account -> accountSynchronizer.synchronize(account, categorizer,
        headers))
      .toArray(CompletableFuture[]::new));
  }

  private Set<UUID> validConnectionIds(List<BankConnection> connections) {
    return connections.stream().filter(BankConnection::isValid)
      .map(BankConnection::id).collect(Collectors.toSet());
  }
}