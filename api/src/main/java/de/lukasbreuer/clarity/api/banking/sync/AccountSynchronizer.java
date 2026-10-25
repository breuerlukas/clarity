package de.lukasbreuer.clarity.api.banking.sync;

import de.lukasbreuer.clarity.api.banking.EnableBankingClient;
import de.lukasbreuer.clarity.api.banking.EnableBankingException;
import de.lukasbreuer.clarity.api.banking.account.BankAccount;
import de.lukasbreuer.clarity.api.banking.account.BankAccountRepository;
import de.lukasbreuer.clarity.api.log.Log;
import de.lukasbreuer.clarity.api.transaction.TransactionRepository;
import de.lukasbreuer.clarity.api.transaction.category.Categorizer;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Brings one account up to date: balance first, then every transaction since
 * the last successful sync (minus a ten day overlap for late bookings). The
 * first sync asks for two years; banks cut that down to what they allow.
 * Failures are recorded on the account instead of thrown, so one broken bank
 * never stops the others.
 */
@Component
@RequiredArgsConstructor
public final class AccountSynchronizer {
  private static final int INITIAL_HISTORY_DAYS = 730;
  private static final int OVERLAP_DAYS = 10;

  private final Log log;
  private final EnableBankingClient client;
  private final BankAccountRepository accountRepository;
  private final TransactionRepository transactionRepository;

  public CompletableFuture<BankAccount> synchronize(
    BankAccount account, Categorizer categorizer, Map<String, String> headers
  ) {
    var from = historyStart(account);
    return updateBalance(account, headers)
      .thenCompose(_ -> TransactionHistory.create(client, headers)
        .fetch(account.uid(), from))
      .thenApply(transactions -> adoptBookedCurrency(account, transactions))
      .thenCompose(transactions -> TransactionImport.create(
        transactionRepository, categorizer).store(account, from, transactions))
      .thenApply(_ -> succeeded(account))
      .exceptionally(throwable -> failed(account, throwable))
      .thenCompose(accountRepository::save);
  }

  private LocalDate historyStart(BankAccount account) {
    if (account.lastSyncedAt() == 0) {
      return LocalDate.now(ZoneOffset.UTC).minusDays(INITIAL_HISTORY_DAYS);
    }
    return LocalDate.ofInstant(Instant.ofEpochMilli(account.lastSyncedAt()),
      ZoneOffset.UTC).minusDays(OVERLAP_DAYS);
  }

  private CompletableFuture<Optional<BigDecimal>> updateBalance(
    BankAccount account, Map<String, String> headers
  ) {
    return client.get("/accounts/" + account.uid() + "/balances", headers)
      .thenApply(response -> BalanceSelection.of(response
        .optJSONArray("balances", new JSONArray())))
      .thenApply(balances -> applyBalance(account, balances));
  }

  private Optional<BigDecimal> applyBalance(
    BankAccount account, BalanceSelection balances
  ) {
    balances.currency().ifPresent(account::adoptBalanceCurrency);
    balances.select().ifPresent(account::updateBalance);
    return balances.select();
  }

  /**
   * When neither the account nor its balance names a currency, the one its
   * latest booking was made in does.
   */
  private List<JSONObject> adoptBookedCurrency(
    BankAccount account, List<JSONObject> transactions
  ) {
    transactions.stream().map(EnableBankingTransaction::of)
      .max(Comparator.comparing(EnableBankingTransaction::bookingDate))
      .map(EnableBankingTransaction::currency)
      .ifPresent(account::adoptBalanceCurrency);
    return transactions;
  }

  private BankAccount succeeded(BankAccount account) {
    account.markSynced();
    return account;
  }

  private BankAccount failed(BankAccount account, Throwable throwable) {
    var reason = failureReason(throwable);
    log.warning("Sync of an account failed: " + reason);
    account.markFailed(reason);
    return account;
  }

  private String failureReason(Throwable throwable) {
    var cause = throwable instanceof CompletionException ?
      throwable.getCause() : throwable;
    if (cause instanceof EnableBankingException exception) {
      return exception.isConsentProblem() ? "consent" :
        "status_" + exception.status();
    }
    return "unreachable";
  }
}