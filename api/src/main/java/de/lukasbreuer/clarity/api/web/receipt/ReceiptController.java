package de.lukasbreuer.clarity.api.web.receipt;

import de.lukasbreuer.clarity.api.transaction.receipt.Receipt;
import de.lukasbreuer.clarity.api.transaction.receipt.ReceiptFiles;
import de.lukasbreuer.clarity.api.transaction.receipt.ReceiptRepository;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;
import de.lukasbreuer.clarity.api.web.request.UuidParameter;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Lists the receipts of a space, newest first, and corrects or deletes
 * them. Deleting a receipt removes its file but leaves the transaction it
 * belonged to. Error codes: 5001 invalid space, 8101 unknown receipt, 8102
 * invalid fields.
 */
@RestController
@RequiredArgsConstructor
public final class ReceiptController {
  private final ReceiptRepository receiptRepository;
  private final ReceiptFiles receiptFiles;

  @RequestMapping(path = "/receipts/", method = RequestMethod.GET)
  public CompletableFuture<ApiResponse> findReceipts(
    @RequestParam("space") String space
  ) {
    var spaceId = UuidParameter.parse(space);
    if (spaceId.isEmpty()) {
      return ApiResponse.error(5001).future();
    }
    return receiptRepository.findBySpaceId(spaceId.get())
      .thenApply(receipts -> ApiResponse.success(Map.of("receipts",
        receipts.stream()
          .sorted(Comparator.comparingLong(Receipt::createdAt).reversed())
          .map(Receipt::information)
          .toList())));
  }

  @RequestMapping(path = "/receipts/update/", method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> update(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var receiptId = body.getUUID("id");
    var draft = ReceiptDraft.parse(body);
    if (receiptId == null) {
      return ApiResponse.error(8101).future();
    }
    if (draft.isEmpty()) {
      return ApiResponse.error(8102).future();
    }
    return receiptRepository.findById(receiptId)
      .thenCompose(receipt -> update(receipt, draft.get()));
  }

  @RequestMapping(path = "/receipts/delete/", method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> delete(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var receiptId = ApiRequestBody.of(payload, response).getUUID("id");
    if (receiptId == null) {
      return ApiResponse.error(8101).future();
    }
    return receiptRepository.findById(receiptId).thenCompose(this::delete);
  }

  private CompletableFuture<ApiResponse> update(
    Optional<Receipt> receipt, ReceiptDraft draft
  ) {
    if (receipt.isEmpty()) {
      return ApiResponse.error(8101).future();
    }
    draft.applyTo(receipt.get());
    return receiptRepository.save(receipt.get())
      .thenApply(saved -> ApiResponse.success(Map.of("receipt",
        saved.information())));
  }

  private CompletableFuture<ApiResponse> delete(Optional<Receipt> receipt) {
    if (receipt.isEmpty()) {
      return ApiResponse.error(8101).future();
    }
    try {
      receiptFiles.delete(receipt.get().id());
    } catch (IOException exception) {
      return CompletableFuture.failedFuture(exception);
    }
    return receiptRepository.delete(receipt.get())
      .thenApply(_ -> ApiResponse.success());
  }
}