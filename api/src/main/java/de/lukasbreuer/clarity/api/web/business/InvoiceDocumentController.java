package de.lukasbreuer.clarity.api.web.business;

import de.lukasbreuer.clarity.api.business.invoice.Invoice;
import de.lukasbreuer.clarity.api.business.invoice.InvoiceDocument;
import de.lukasbreuer.clarity.api.business.invoice.InvoiceDocumentRepository;
import de.lukasbreuer.clarity.api.business.invoice.InvoiceRepository;
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
 * The original file of an invoice, always as a download and never rendered
 * in the panel's origin, since anyone can mail a file. Unknown invoices and
 * invoices without a file answer 404.
 */
@RestController
@RequiredArgsConstructor
public final class InvoiceDocumentController {
  private final InvoiceRepository invoiceRepository;
  private final InvoiceDocumentRepository documentRepository;

  @RequestMapping(path = "/business/invoices/document/",
    method = RequestMethod.GET)
  public CompletableFuture<ResponseEntity<byte[]>> findDocument(
    @RequestParam("id") String id
  ) {
    var invoiceId = UuidParameter.parse(id);
    if (invoiceId.isEmpty()) {
      return CompletableFuture.completedFuture(notFound());
    }
    return invoiceRepository.findById(invoiceId.get())
      .thenCompose(this::document)
      .thenApply(document -> document.map(this::download)
        .orElseGet(this::notFound));
  }

  private CompletableFuture<Optional<InvoiceDocument>> document(
    Optional<Invoice> invoice
  ) {
    return invoice.map(Invoice::documentId)
      .map(documentRepository::findById)
      .orElse(CompletableFuture.completedFuture(Optional.empty()));
  }

  private ResponseEntity<byte[]> download(InvoiceDocument document) {
    return ResponseEntity.ok()
      .contentType(MediaType.parseMediaType(document.contentType()))
      .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
        .filename(document.name(), StandardCharsets.UTF_8).build().toString())
      .header("X-Content-Type-Options", "nosniff")
      .header("Content-Security-Policy", "sandbox")
      .header(HttpHeaders.CACHE_CONTROL, "no-store")
      .body(document.content());
  }

  private ResponseEntity<byte[]> notFound() {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
  }
}