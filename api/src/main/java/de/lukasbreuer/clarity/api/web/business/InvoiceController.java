package de.lukasbreuer.clarity.api.web.business;

import de.lukasbreuer.clarity.api.business.invoice.Invoice;
import de.lukasbreuer.clarity.api.business.invoice.InvoicePaymentRepository;
import de.lukasbreuer.clarity.api.business.invoice.InvoiceRepository;
import de.lukasbreuer.clarity.api.business.invoice.matching.PaymentReconciliation;
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
  private final PaymentReconciliation reconciliation;
  private final InvoiceResponses responses;

  @RequestMapping(path = "/business/invoices/", method = RequestMethod.GET)
  public CompletableFuture<ApiResponse> findInvoices(
    @RequestParam("space") String space
  ) {
    var spaceId = UuidParameter.parse(space);
    if (spaceId.isEmpty()) {
      return ApiResponse.error(5001).future();
    }
    return invoiceRepository.findBySpaceId(spaceId.get())
      .thenApply(responses::list);
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
    return invoiceRepository.findById(invoiceId).thenCompose(this::remove);
  }

  private CompletableFuture<ApiResponse> remove(Optional<Invoice> invoice) {
    if (invoice.isEmpty()) {
      return ApiResponse.error(7001).future();
    }
    paymentRepository.deleteByInvoiceId(invoice.get().id());
    return invoiceRepository.delete(invoice.get())
      .thenApply(_ -> ApiResponse.success());
  }

  private CompletableFuture<ApiResponse> change(
    UUID invoiceId, Consumer<Invoice> change
  ) {
    return invoiceRepository.findById(invoiceId)
      .thenCompose(invoice -> invoice.isEmpty() ?
        ApiResponse.error(7001).future() : save(invoice.get(), change));
  }

  private CompletableFuture<ApiResponse> save(
    Invoice invoice, Consumer<Invoice> change
  ) {
    change.accept(invoice);
    return invoiceRepository.save(invoice).thenCompose(this::matchAgain);
  }

  private CompletableFuture<ApiResponse> matchAgain(Invoice invoice) {
    reconciliation.reconcile(invoice.spaceId());
    return responses.current(invoice.id());
  }
}