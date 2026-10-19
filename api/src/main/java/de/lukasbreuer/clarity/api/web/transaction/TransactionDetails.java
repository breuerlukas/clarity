package de.lukasbreuer.clarity.api.web.transaction;

import de.lukasbreuer.clarity.api.transaction.Transaction;
import de.lukasbreuer.clarity.api.transaction.receipt.Receipt;
import de.lukasbreuer.clarity.api.transaction.receipt.ReceiptRepository;
import de.lukasbreuer.clarity.api.transaction.split.TransactionSplit;
import de.lukasbreuer.clarity.api.transaction.split.TransactionSplitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Transactions as the panel lists them: each with the parts it is split
 * into and its receipt, if it has one.
 */
@Component
@RequiredArgsConstructor
final class TransactionDetails {
  private final TransactionSplitRepository splitRepository;
  private final ReceiptRepository receiptRepository;

  CompletableFuture<List<Map<String, Object>>> information(
    List<Transaction> transactions
  ) {
    var transactionIds = transactions.stream().map(Transaction::id).toList();
    return splitRepository.findOfTransactions(transactionIds)
      .thenCombine(receiptRepository.findOfTransactions(transactionIds),
        (splits, receipts) -> information(transactions, splits, receipts));
  }

  private List<Map<String, Object>> information(
    List<Transaction> transactions, List<TransactionSplit> splits,
    List<Receipt> receipts
  ) {
    var splitsByTransaction = splits.stream()
      .collect(Collectors.groupingBy(TransactionSplit::transactionId));
    var receiptByTransaction = receipts.stream()
      .collect(Collectors.toMap(Receipt::transactionId, Receipt::id,
        (first, _) -> first));
    return transactions.stream()
      .map(transaction -> information(transaction,
        splitsByTransaction.getOrDefault(transaction.id(), List.of()),
        receiptByTransaction.get(transaction.id())))
      .toList();
  }

  private Map<String, Object> information(
    Transaction transaction, List<TransactionSplit> splits, UUID receiptId
  ) {
    var information = transaction.information();
    information.put("splits", splits.stream()
      .map(TransactionSplit::information).toList());
    information.put("receipt", receiptId);
    return information;
  }
}