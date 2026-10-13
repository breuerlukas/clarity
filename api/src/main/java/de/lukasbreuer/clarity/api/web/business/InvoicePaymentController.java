package de.lukasbreuer.clarity.api.web.business;

import de.lukasbreuer.clarity.api.banking.account.AccountScope;
import de.lukasbreuer.clarity.api.business.invoice.Invoice;
import de.lukasbreuer.clarity.api.business.invoice.InvoiceDirection;
import de.lukasbreuer.clarity.api.business.invoice.InvoicePayment;
import de.lukasbreuer.clarity.api.business.invoice.InvoicePaymentRepository;
import de.lukasbreuer.clarity.api.business.invoice.InvoiceRepository;
import de.lukasbreuer.clarity.api.business.invoice.matching.InvoiceMatcher;
import de.lukasbreuer.clarity.api.business.invoice.matching.PaymentMatching;
import de.lukasbreuer.clarity.api.transaction.Transaction;
import de.lukasbreuer.clarity.api.transaction.TransactionRepository;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Links bookings to invoices by hand, removes links and suggests bookings
 * that may pay an invoice. Error codes: 7001 unknown invoice, 7002 unknown
 * transaction or one that cannot pay this invoice, 7003 unknown payment.
 */
@RestController
@RequiredArgsConstructor
public final class InvoicePaymentController {
  private final InvoiceRepository invoiceRepository;
  private final InvoicePaymentRepository paymentRepository;
  private final TransactionRepository transactionRepository;
  private final InvoiceMatcher invoiceMatcher;
  private final InvoiceController invoiceController;
  private final AccountScope accountScope;

  @RequestMapping(path = "/business/invoices/suggestions/",
    method = RequestMethod.GET)
  public CompletableFuture<ApiResponse> suggest(
    @RequestParam("id") String id
  ) {
    var invoiceId = UuidParameter.parse(id);
    if (invoiceId.isEmpty()) {
      return ApiResponse.error(7001).future();
    }
    return invoiceRepository.findById(invoiceId.get())
      .thenApply(invoice -> invoice.isEmpty() ? ApiResponse.error(7001) :
        ApiResponse.success(Map.of("suggestions",
          invoiceMatcher.suggest(invoice.get()).stream()
            .map(suggestion -> Map.of(
              "transaction", suggestion.transaction().information(),
              "reasons", suggestion.reasons().stream().map(Enum::name)
                .toList()))
            .toList())));
  }

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
    return invoiceRepository.findById(invoiceId)
      .thenCompose(invoice -> invoice.isEmpty() ?
        ApiResponse.error(7001).future() :
        transactionRepository.findById(transactionId)
          .thenCompose(transaction -> transaction.isEmpty() ?
            ApiResponse.error(7002).future() :
            link(invoice.get(), transaction.get())));
  }

  private CompletableFuture<ApiResponse> link(
    Invoice invoice, Transaction transaction
  ) {
    var incoming = transaction.amount().signum() > 0;
    if (incoming != (invoice.direction() == InvoiceDirection.OUTGOING)) {
      return ApiResponse.error(7002).future();
    }
    return accountScope.accountIds(invoice.spaceId())
      .thenCompose(accountIds -> {
        if (!accountIds.contains(transaction.accountId())) {
          return ApiResponse.error(7002).future();
        }
        var invoicePayments = invoiceMatcher.payments(List.of(invoice));
        if (invoicePayments.stream().anyMatch(payment ->
          payment.transactionId().equals(transaction.id()) &&
            !payment.rejected())) {
          return ApiResponse.error(7002).future();
        }
        var transactionPayments = paymentRepository.findByTransactionIdIn(
          List.of(transaction.id())).join();
        var amount = invoice.openAmount(invoicePayments).min(
          PaymentMatching.remaining(transaction, transactionPayments));
        if (amount.signum() <= 0) {
          return ApiResponse.error(7002).future();
        }
        invoicePayments.stream()
          .filter(payment -> payment.transactionId().equals(transaction.id()))
          .forEach(payment -> paymentRepository.delete(payment).join());
        return paymentRepository.save(payment(invoice, transaction, amount))
          .thenApply(_ -> invoiceController.answer(invoice.id()));
      });
  }

  private InvoicePayment payment(
    Invoice invoice, Transaction transaction, BigDecimal amount
  ) {
    return InvoicePayment.create(UUID.randomUUID(), invoice.id(),
      transaction.id(), transaction.accountId(), amount,
      transaction.bookingDate(), false, false, System.currentTimeMillis());
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
    return paymentRepository.findById(paymentId)
      .thenCompose(payment -> {
        if (payment.isEmpty() || payment.get().rejected()) {
          return ApiResponse.error(7003).future();
        }
        if (!payment.get().automatic()) {
          return paymentRepository.delete(payment.get())
            .thenApply(_ -> invoiceController.answer(
              payment.get().invoiceId()));
        }
        payment.get().reject();
        return paymentRepository.save(payment.get())
          .thenApply(_ -> invoiceController.answer(payment.get().invoiceId()));
      });
  }
}
