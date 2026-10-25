package de.lukasbreuer.clarity.api.web.cash;

import de.lukasbreuer.clarity.api.space.SpaceRepository;
import de.lukasbreuer.clarity.api.transaction.Transaction;
import de.lukasbreuer.clarity.api.transaction.TransactionRepository;
import de.lukasbreuer.clarity.api.transaction.cash.CashWallets;
import de.lukasbreuer.clarity.api.transaction.category.CategoryRepository;
import de.lukasbreuer.clarity.api.transaction.receipt.ReceiptRepository;
import de.lukasbreuer.clarity.api.transaction.split.TransactionSplitRepository;
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
 * Books, changes and deletes cash entries, money spent or received in cash.
 * Only entries of a wallet can be changed here. A new amount undoes a
 * split, and a deleted entry takes its split along and leaves its receipt
 * unassigned. Error codes: 3002 unknown
 * category, 5001 unknown space, 8000 invalid entry, 8001 unknown entry.
 */
@RestController
@RequiredArgsConstructor
public final class CashEntryController {
  private final SpaceRepository spaceRepository;
  private final CategoryRepository categoryRepository;
  private final TransactionRepository transactionRepository;
  private final TransactionSplitRepository splitRepository;
  private final ReceiptRepository receiptRepository;
  private final CashWallets wallets;

  @RequestMapping(path = "/cash/entries/create/", method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> create(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var spaceId = body.getUUID("space");
    var draft = CashEntryDraft.parse(body);
    if (spaceId == null) {
      return ApiResponse.error(5001).future();
    }
    if (draft.isEmpty()) {
      return ApiResponse.error(8000).future();
    }
    return spaceRepository.existsById(spaceId)
      .thenCompose(exists -> exists ? createInSpace(spaceId, draft.get()) :
        ApiResponse.error(5001).future());
  }

  @RequestMapping(path = "/cash/entries/update/", method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> update(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var entryId = body.getUUID("id");
    var draft = CashEntryDraft.parse(body);
    if (entryId == null) {
      return ApiResponse.error(8001).future();
    }
    if (draft.isEmpty()) {
      return ApiResponse.error(8000).future();
    }
    return categoryRepository.existsById(draft.get().category())
      .thenCompose(exists -> exists ? wallets.entry(entryId)
        .thenCompose(entry -> update(entry, draft.get())) :
        ApiResponse.error(3002).future());
  }

  @RequestMapping(path = "/cash/entries/delete/", method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> delete(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var entryId = ApiRequestBody.of(payload, response).getUUID("id");
    if (entryId == null) {
      return ApiResponse.error(8001).future();
    }
    return wallets.entry(entryId).thenCompose(this::delete);
  }

  private CompletableFuture<ApiResponse> createInSpace(
    UUID spaceId, CashEntryDraft draft
  ) {
    return categoryRepository.existsById(draft.category())
      .thenCompose(exists -> exists ? wallets.of(spaceId)
        .thenCompose(wallet -> transactionRepository.save(
          draft.create(wallet)))
        .thenApply(this::entryResponse) : ApiResponse.error(3002).future());
  }

  private CompletableFuture<ApiResponse> update(
    Optional<Transaction> entry, CashEntryDraft draft
  ) {
    if (entry.isEmpty()) {
      return ApiResponse.error(8001).future();
    }
    keepSplitFitting(entry.get(), draft);
    draft.applyTo(entry.get());
    return transactionRepository.save(entry.get())
      .thenApply(this::entryResponse);
  }

  /**
   * Parts of the old amount no longer add up to a new one.
   */
  private void keepSplitFitting(Transaction entry, CashEntryDraft draft) {
    if (entry.amount().compareTo(draft.amount()) != 0) {
      splitRepository.deleteByTransactionId(entry.id());
    }
  }

  private CompletableFuture<ApiResponse> delete(Optional<Transaction> entry) {
    if (entry.isEmpty()) {
      return ApiResponse.error(8001).future();
    }
    splitRepository.deleteByTransactionId(entry.get().id());
    receiptRepository.detachFrom(entry.get().id());
    return transactionRepository.delete(entry.get())
      .thenApply(_ -> ApiResponse.success());
  }

  private ApiResponse entryResponse(Transaction entry) {
    return ApiResponse.success(Map.of("transaction", entry.information()));
  }
}