package de.lukasbreuer.clarity.api.web.receipt;

import de.lukasbreuer.clarity.api.space.SpaceRepository;
import de.lukasbreuer.clarity.api.transaction.receipt.Receipt;
import de.lukasbreuer.clarity.api.transaction.receipt.ReceiptFiles;
import de.lukasbreuer.clarity.api.transaction.receipt.ReceiptReader;
import de.lukasbreuer.clarity.api.transaction.receipt.ReceiptRepository;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Takes a photo or PDF of a receipt, reads it and keeps it unassigned until
 * the owner has reviewed what was read. Tesseract takes a few seconds, so
 * reading runs off the request thread. Error codes: 5001 unknown space,
 * 8100 no photo or PDF, or larger than 10 MB.
 */
@RestController
@RequiredArgsConstructor
public final class ReceiptScanController {
  private final SpaceRepository spaceRepository;
  private final ReceiptRepository receiptRepository;
  private final ReceiptReader receiptReader;
  private final ReceiptFiles receiptFiles;

  @RequestMapping(path = "/receipts/scan/", method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> scan(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var spaceId = body.getUUID("space");
    var upload = ReceiptUpload.parse(body);
    if (spaceId == null) {
      return ApiResponse.error(5001).future();
    }
    if (upload.isEmpty()) {
      return ApiResponse.error(8100).future();
    }
    return spaceRepository.existsById(spaceId)
      .thenCompose(exists -> exists ? scan(spaceId, upload.get()) :
        ApiResponse.error(5001).future());
  }

  private CompletableFuture<ApiResponse> scan(
    UUID spaceId, ReceiptUpload upload
  ) {
    return CompletableFuture.supplyAsync(() -> receiptReader
        .read(upload.file()))
      .thenApply(recognised -> upload.receipt(spaceId, recognised))
      .thenCompose(receipt -> store(receipt, upload))
      .thenApply(receipt -> ApiResponse.success(Map.of("receipt",
        receipt.information())));
  }

  private CompletableFuture<Receipt> store(
    Receipt receipt, ReceiptUpload upload
  ) {
    try {
      receiptFiles.store(receipt.id(), upload.file().content());
    } catch (IOException exception) {
      return CompletableFuture.failedFuture(exception);
    }
    return receiptRepository.save(receipt);
  }
}