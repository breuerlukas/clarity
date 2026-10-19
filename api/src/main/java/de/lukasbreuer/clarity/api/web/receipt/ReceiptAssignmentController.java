package de.lukasbreuer.clarity.api.web.receipt;

import de.lukasbreuer.clarity.api.banking.account.AccountScope;
import de.lukasbreuer.clarity.api.transaction.Transaction;
import de.lukasbreuer.clarity.api.transaction.TransactionRepository;
import de.lukasbreuer.clarity.api.transaction.cash.CashWallets;
import de.lukasbreuer.clarity.api.transaction.category.CategoryRepository;
import de.lukasbreuer.clarity.api.transaction.receipt.Receipt;
import de.lukasbreuer.clarity.api.transaction.receipt.ReceiptRepository;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Assigns a receipt: to a transaction of its space, usually the card
 * payment, or as a new cash expense over its total, with its merchant as
 * the counterparty. Error codes: 3002 unknown category, 8101 unknown
 * receipt, 8103 unknown transaction or one of another space, 8104 receipt
 * without date or total.
 */
@RestController
@RequiredArgsConstructor
public final class ReceiptAssignmentController {
  private final ReceiptRepository receiptRepository;
  private final TransactionRepository transactionRepository;
  private final CategoryRepository categoryRepository;
  private final AccountScope accountScope;
  private final CashWallets wallets;

  @RequestMapping(path = "/receipts/attach/", method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> attach(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var receiptId = body.getUUID("id");
    var transactionId = body.getUUID("transaction");
    if (receiptId == null) {
      return ApiResponse.error(8101).future();
    }
    if (transactionId == null) {
      return ApiResponse.error(8103).future();
    }
    return receiptRepository.findById(receiptId)
      .thenCompose(receipt -> attach(receipt, transactionId));
  }

  @RequestMapping(path = "/receipts/cash/", method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> bookAsCash(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var receiptId = body.getUUID("id");
    var category = body.getString("category", 64);
    if (receiptId == null) {
      return ApiResponse.error(8101).future();
    }
    return categoryRepository.existsById(category)
      .thenCompose(exists -> exists ? receiptRepository.findById(receiptId)
        .thenCompose(receipt -> bookAsCash(receipt, category)) :
        ApiResponse.error(3002).future());
  }

  private CompletableFuture<ApiResponse> attach(
    Optional<Receipt> receipt, UUID transactionId
  ) {
    if (receipt.isEmpty()) {
      return ApiResponse.error(8101).future();
    }
    return accountScope.accountIds(receipt.get().spaceId())
      .thenCompose(accountIds -> transactionRepository.findById(transactionId)
        .thenApply(transaction -> transaction.filter(found ->
          accountIds.contains(found.accountId()))))
      .thenCompose(transaction -> transaction.isEmpty() ?
        ApiResponse.error(8103).future() :
        attach(receipt.get(), transaction.get()));
  }

  private CompletableFuture<ApiResponse> bookAsCash(
    Optional<Receipt> receipt, String category
  ) {
    if (receipt.isEmpty()) {
      return ApiResponse.error(8101).future();
    }
    if (!receipt.get().hasDateAndTotal()) {
      return ApiResponse.error(8104).future();
    }
    return wallets.of(receipt.get().spaceId())
      .thenCompose(wallet -> transactionRepository.save(wallet.entry(
        receipt.get().purchaseDate(), receipt.get().total().negate(),
        receipt.get().merchant(), "", category)))
      .thenCompose(entry -> attach(receipt.get(), entry));
  }

  private CompletableFuture<ApiResponse> attach(
    Receipt receipt, Transaction transaction
  ) {
    receipt.attachTo(transaction.id());
    return receiptRepository.save(receipt)
      .thenApply(saved -> ApiResponse.success(Map.of(
        "receipt", saved.information(),
        "transaction", transaction.information())));
  }
}