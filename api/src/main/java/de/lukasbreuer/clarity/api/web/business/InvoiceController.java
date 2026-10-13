package de.lukasbreuer.clarity.api.web.business;

import de.lukasbreuer.clarity.api.business.invoice.Invoice;
import de.lukasbreuer.clarity.api.business.invoice.InvoiceDocumentRepository;
import de.lukasbreuer.clarity.api.business.invoice.InvoicePaymentRepository;
import de.lukasbreuer.clarity.api.business.invoice.InvoiceRepository;
import de.lukasbreuer.clarity.api.business.invoice.matching.InvoiceMatcher;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;
import de.lukasbreuer.clarity.api.web.request.UuidParameter;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * The invoices of a business space as read from its mailboxes, with what
 * paid them. The owner corrects what was read, settles invoices paid
 * otherwise and deletes what is no invoice at all. Error codes: 7000
 * invalid invoice, 7001 unknown invoice, 5001 unknown space.
 */
@RestController
@RequiredArgsConstructor
public final class InvoiceController {
  private final InvoiceRepository invoiceRepository;
  private final InvoicePaymentRepository paymentRepository;
  private final InvoiceDocumentRepository documentRepository;
  private final InvoiceMatcher invoiceMatcher;

  @RequestMapping(path = "/business/invoices/", method = RequestMethod.GET)
  public CompletableFuture<ApiResponse> findInvoices(
    @RequestParam("space") String space
  ) {
    var spaceId = UuidParameter.parse(space);
    if (spaceId.isEmpty()) {
      return ApiResponse.error(5001).future();
    }
    return invoiceRepository.findBySpaceId(spaceId.get())
      .thenApply(invoices -> {
        var payments = invoiceMatcher.payments(invoices);
        var today = LocalDate.now();
        return ApiResponse.success(Map.of("invoices", invoices.stream()
          .sorted(Comparator.comparing(Invoice::invoiceDate).reversed())
          .map(invoice -> invoice.information(payments, today))
          .toList()));
      });
  }

  /**
   * The original file, always as a download and never rendered in the
   * panel's origin, since anyone can mail a file.
   */
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
      .thenCompose(invoice -> invoice.map(Invoice::documentId)
        .map(documentRepository::findById)
        .orElse(CompletableFuture.completedFuture(Optional.empty())))
      .thenApply(document -> document.map(entry -> ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(entry.contentType()))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition
          .attachment().filename(entry.name(), StandardCharsets.UTF_8)
          .build().toString())
        .header("X-Content-Type-Options", "nosniff")
        .header("Content-Security-Policy", "sandbox")
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .body(entry.content())).orElseGet(this::notFound));
  }

  private ResponseEntity<byte[]> notFound() {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
  }

  /**
   * Saves the owner's corrections, which also confirms what was read, and
   * matches payments again with them.
   */
  @RequestMapping(path = "/business/invoices/update/",
    method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> update(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var invoiceId = body.getUUID("id");
    var draft = InvoiceDraft.parse(body);
    if (invoiceId == null) {
      return ApiResponse.error(7001).future();
    }
    if (draft.isEmpty()) {
      return ApiResponse.error(7000).future();
    }
    return change(invoiceId, invoice -> invoice.edit(draft.get().direction(),
      draft.get().fields()));
  }

  @RequestMapping(path = "/business/invoices/settle/",
    method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> settle(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var invoiceId = body.getUUID("id");
    var settled = body.getBoolean("settled");
    if (invoiceId == null) {
      return ApiResponse.error(7001).future();
    }
    return change(invoiceId, invoice -> invoice.settle(settled));
  }

  /**
   * For mails that were taken for an invoice by mistake; the links to
   * bookings go with it.
   */
  @RequestMapping(path = "/business/invoices/delete/",
    method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> delete(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var invoiceId = ApiRequestBody.of(payload, response).getUUID("id");
    if (invoiceId == null) {
      return ApiResponse.error(7001).future();
    }
    return invoiceRepository.findById(invoiceId)
      .thenCompose(invoice -> {
        if (invoice.isEmpty()) {
          return ApiResponse.error(7001).future();
        }
        paymentRepository.deleteByInvoiceId(invoice.get().id());
        return invoiceRepository.delete(invoice.get())
          .thenApply(_ -> ApiResponse.success());
      });
  }

  private CompletableFuture<ApiResponse> change(
    UUID invoiceId, Consumer<Invoice> change
  ) {
    return invoiceRepository.findById(invoiceId)
      .thenCompose(invoice -> {
        if (invoice.isEmpty()) {
          return ApiResponse.error(7001).future();
        }
        change.accept(invoice.get());
        return invoiceRepository.save(invoice.get())
          .thenApply(saved -> {
            invoiceMatcher.match(saved.spaceId());
            return answer(saved.id());
          });
      });
  }

  /**
   * The invoice as stored now, after the matching may have changed it.
   */
  ApiResponse answer(UUID invoiceId) {
    var invoice = invoiceRepository.findById(invoiceId).join().orElseThrow();
    var payments = invoiceMatcher.payments(List.of(invoice));
    return ApiResponse.success(Map.of("invoice",
      invoice.information(payments, LocalDate.now())));
  }
}
