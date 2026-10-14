package de.lukasbreuer.clarity.api.web.business;

import de.lukasbreuer.clarity.api.business.invoice.InvoicePayment;
import de.lukasbreuer.clarity.api.business.invoice.InvoicePaymentRepository;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Links bookings to invoices by hand and removes links. Error codes: 7001
 * unknown invoice, 7002 unknown transaction or one that cannot pay this
 * invoice, 7003 unknown payment.
 */
@RestController
@RequiredArgsConstructor
public final class InvoicePaymentController {
  private final InvoicePaymentRepository paymentRepository;
  private final InvoicePaymentLinking linking;
  private final InvoiceResponses responses;

  /**
   * Applies as much of the booking as the invoice still needs and the
   * booking has left; a link removed before counts again.
   */
  @RequestMapping(path = "/business/invoices/payments/link/",
    method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> link(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var invoiceId = body.getUUID("invoice");
    var transactionId = body.getUUID("transaction");
    if (invoiceId == null) {
      return ApiResponse.error(7001).future();
    }
    if (transactionId == null) {
      return ApiResponse.error(7002).future();
    }
    return linking.link(invoiceId, transactionId);
  }

  /**
   * A link the automatic match made is kept as rejected, so the match does
   * not make it again; one made by hand simply goes.
   */
  @RequestMapping(path = "/business/invoices/payments/unlink/",
    method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> unlink(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var paymentId = ApiRequestBody.of(payload, response).getUUID("id");
    if (paymentId == null) {
      return ApiResponse.error(7003).future();
    }
    return paymentRepository.findById(paymentId).thenCompose(this::unlink);
  }

  private CompletableFuture<ApiResponse> unlink(
    Optional<InvoicePayment> payment
  ) {
    if (payment.isEmpty() || payment.get().rejected()) {
      return ApiResponse.error(7003).future();
    }
    var invoiceId = payment.get().invoiceId();
    return removeOrReject(payment.get())
      .thenCompose(_ -> responses.current(invoiceId));
  }

  private CompletableFuture<?> removeOrReject(InvoicePayment payment) {
    if (!payment.automatic()) {
      return paymentRepository.delete(payment);
    }
    payment.reject();
    return paymentRepository.save(payment);
  }
}