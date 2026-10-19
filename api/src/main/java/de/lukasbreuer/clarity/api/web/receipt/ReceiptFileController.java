package de.lukasbreuer.clarity.api.web.receipt;

import de.lukasbreuer.clarity.api.transaction.receipt.Receipt;
import de.lukasbreuer.clarity.api.transaction.receipt.ReceiptFiles;
import de.lukasbreuer.clarity.api.transaction.receipt.ReceiptRepository;
import de.lukasbreuer.clarity.api.web.request.UuidParameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * The photo or PDF of a receipt, served from this instance only. Like an
 * invoice file it is a download that never runs in the panel's origin; an
 * image element shows it all the same. Unknown receipts and missing files
 * answer 404.
 */
@RestController
@RequiredArgsConstructor
public final class ReceiptFileController {
  private final ReceiptRepository receiptRepository;
  private final ReceiptFiles receiptFiles;

  @RequestMapping(path = "/receipts/file/", method = RequestMethod.GET)
  public CompletableFuture<ResponseEntity<byte[]>> findFile(
    @RequestParam("id") String id
  ) {
    var receiptId = UuidParameter.parse(id);
    if (receiptId.isEmpty()) {
      return CompletableFuture.completedFuture(notFound());
    }
    return receiptRepository.findById(receiptId.get())
      .thenApply(this::file);
  }

  private ResponseEntity<byte[]> file(Optional<Receipt> receipt) {
    return receipt.flatMap(found -> receiptFiles.read(found.id())
        .map(content -> download(found, content)))
      .orElseGet(this::notFound);
  }

  private ResponseEntity<byte[]> download(Receipt receipt, byte[] content) {
    return ResponseEntity.ok()
      .contentType(MediaType.parseMediaType(receipt.contentType()))
      .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
        .filename(receipt.fileName(), StandardCharsets.UTF_8).build()
        .toString())
      .header("X-Content-Type-Options", "nosniff")
      .header("Content-Security-Policy", "sandbox")
      .header(HttpHeaders.CACHE_CONTROL, "no-store")
      .body(content);
  }

  private ResponseEntity<byte[]> notFound() {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
  }
}