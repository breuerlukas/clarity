package de.lukasbreuer.clarity.api.web.receipt;

import de.lukasbreuer.clarity.api.banking.account.AccountScope;
import de.lukasbreuer.clarity.api.transaction.Transaction;
import de.lukasbreuer.clarity.api.transaction.TransactionRepository;
import de.lukasbreuer.clarity.api.transaction.cash.CashWallets;
import de.lukasbreuer.clarity.api.transaction.receipt.Receipt;
import de.lukasbreuer.clarity.api.transaction.receipt.ReceiptMatching;
import de.lukasbreuer.clarity.api.transaction.receipt.ReceiptRepository;
import de.lukasbreuer.clarity.api.web.request.UuidParameter;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * The card payments of the receipt's space it may belong to, see
 * {@link ReceiptMatching}; none while the receipt has no date or total.
 * Error code 8101: unknown receipt.
 */
@RestController
@RequiredArgsConstructor
public final class ReceiptSuggestionController {
  private final ReceiptRepository receiptRepository;
  private final TransactionRepository transactionRepository;
  private final AccountScope accountScope;
  private final CashWallets wallets;

  @RequestMapping(path = "/receipts/suggestions/", method = RequestMethod.GET)
  public CompletableFuture<ApiResponse> findSuggestions(
    @RequestParam("id") String id
  ) {
    var receiptId = UuidParameter.parse(id);
    if (receiptId.isEmpty()) {
      return ApiResponse.error(8101).future();
    }
    return receiptRepository.findById(receiptId.get())
      .thenCompose(this::suggestions);
  }

  private CompletableFuture<ApiResponse> suggestions(
    Optional<Receipt> receipt
  ) {
    if (receipt.isEmpty()) {
      return ApiResponse.error(8101).future();
    }
    if (!receipt.get().hasDateAndTotal()) {
      return suggestionResponse(List.of()).future();
    }
    return candidates(receipt.get())
      .thenCompose(transactions -> suggestions(receipt.get(), transactions))
      .thenApply(this::suggestionResponse);
  }

  private CompletableFuture<List<Transaction>> candidates(Receipt receipt) {
    var matching = ReceiptMatching.of(receipt);
    return accountScope.accountIds(receipt.spaceId())
      .thenCompose(accountIds -> transactionRepository.findByAccountsBetween(
        accountIds, matching.earliestBooking(), matching.latestBooking()));
  }

  private CompletableFuture<List<Transaction>> suggestions(
    Receipt receipt, List<Transaction> transactions
  ) {
    return wallets.of(receipt.spaceId())
      .thenCombine(withReceipt(transactions),
        (wallet, withReceipt) -> ReceiptMatching.of(receipt)
          .suggestions(transactions, wallet.id(), withReceipt));
  }

  private CompletableFuture<Set<UUID>> withReceipt(
    List<Transaction> transactions
  ) {
    return receiptRepository.findOfTransactions(transactions.stream()
        .map(Transaction::id).toList())
      .thenApply(receipts -> receipts.stream().map(Receipt::transactionId)
        .collect(Collectors.toSet()));
  }

  private ApiResponse suggestionResponse(List<Transaction> transactions) {
    return ApiResponse.success(Map.of("suggestions", transactions.stream()
      .map(Transaction::information).toList()));
  }
}