package de.lukasbreuer.clarity.api.banking.sync;

import de.lukasbreuer.clarity.api.banking.account.BankAccount;
import de.lukasbreuer.clarity.api.transaction.Transaction;
import de.lukasbreuer.clarity.api.transaction.TransactionRepository;
import de.lukasbreuer.clarity.api.transaction.category.Categorizer;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Stores the transactions of one sync that are not stored yet. Two identical
 * coffees on the same day can share a content hash, so repeated references
 * within one batch get a running suffix ("#2") instead of being dropped.
 */
@RequiredArgsConstructor(staticName = "create")
public final class TransactionImport {
  private final TransactionRepository transactionRepository;
  private final Categorizer categorizer;

  public CompletableFuture<Integer> store(
    BankAccount account, LocalDate from, List<JSONObject> rawTransactions
  ) {
    var fresh = rawTransactions.stream()
      .map(EnableBankingTransaction::of)
      .filter(EnableBankingTransaction::isBooked)
      .map(transaction -> transaction.toTransaction(account.id(), categorizer))
      .toList();
    return transactionRepository.findByAccountIdAndBookingDateGreaterThanEqual(
        account.id(), from)
      .thenApply(existing -> existing.stream().map(Transaction::externalId)
        .collect(Collectors.toSet()))
      .thenCompose(known -> storeUnknown(withUniqueReferences(fresh), known));
  }

  private CompletableFuture<Integer> storeUnknown(
    List<Transaction> transactions, Set<String> knownReferences
  ) {
    var unknown = transactions.stream()
      .filter(transaction -> !knownReferences.contains(
        transaction.externalId()))
      .toList();
    return transactionRepository.saveAll(unknown).thenApply(List::size);
  }

  private List<Transaction> withUniqueReferences(
    List<Transaction> transactions
  ) {
    Map<String, Integer> occurrences = new HashMap<>();
    return transactions.stream()
      .map(transaction -> withOccurrence(transaction, occurrences.merge(
        transaction.externalId(), 1, Integer::sum)))
      .toList();
  }

  private Transaction withOccurrence(Transaction transaction, int occurrence) {
    if (occurrence == 1) {
      return transaction;
    }
    return Transaction.create(transaction.id(), transaction.accountId(),
      transaction.externalId() + "#" + occurrence, transaction.bookingDate(),
      transaction.amount(), transaction.currency(), transaction.counterparty(),
      transaction.counterpartyIban(), transaction.description(),
      transaction.category(), false);
  }
}