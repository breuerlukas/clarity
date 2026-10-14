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
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Links a booking to an invoice by hand. Only money in pays an outgoing
 * invoice and only money out an incoming one, the booking has to be on an
 * account of the invoice's space, and it is applied as far as both the
 * invoice's open amount and what is left of the booking go. Error codes:
 * 7001 unknown invoice, 7002 unknown transaction or one that cannot pay this
 * invoice.
 */
@Component
@RequiredArgsConstructor
final class InvoicePaymentLinking {
  private final InvoiceRepository invoiceRepository;
  private final InvoicePaymentRepository paymentRepository;
  private final TransactionRepository transactionRepository;
  private final InvoiceMatcher invoiceMatcher;
  private final InvoiceResponses responses;
  private final AccountScope accountScope;

  CompletableFuture<ApiResponse> link(UUID invoiceId, UUID transactionId) {
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
      .thenCompose(accountIds -> accountIds.contains(transaction.accountId()) ?
        linkInSpace(invoice, transaction) : ApiResponse.error(7002).future());
  }

  private CompletableFuture<ApiResponse> linkInSpace(
    Invoice invoice, Transaction transaction
  ) {
    var invoicePayments = invoiceMatcher.payments(List.of(invoice));
    if (isLinked(invoicePayments, transaction)) {
      return ApiResponse.error(7002).future();
    }
    var amount = linkableAmount(invoice, invoicePayments, transaction);
    if (amount.signum() <= 0) {
      return ApiResponse.error(7002).future();
    }
    forgetRejection(invoicePayments, transaction);
    return paymentRepository.save(payment(invoice, transaction, amount))
      .thenCompose(_ -> responses.current(invoice.id()));
  }

  private boolean isLinked(
    List<InvoicePayment> invoicePayments, Transaction transaction
  ) {
    return invoicePayments.stream().anyMatch(payment ->
      payment.transactionId().equals(transaction.id()) && !payment.rejected());
  }

  private BigDecimal linkableAmount(
    Invoice invoice, List<InvoicePayment> invoicePayments,
    Transaction transaction
  ) {
    var transactionPayments = paymentRepository.findByTransactionIdIn(
      List.of(transaction.id())).join();
    return invoice.openAmount(invoicePayments).min(PaymentMatching
      .create(List.of(), transactionPayments).remaining(transaction));
  }

  /**
   * A rejected link of the same booking gives way to the new one.
   */
  private void forgetRejection(
    List<InvoicePayment> invoicePayments, Transaction transaction
  ) {
    invoicePayments.stream()
      .filter(payment -> payment.transactionId().equals(transaction.id()))
      .forEach(payment -> paymentRepository.delete(payment).join());
  }

  private InvoicePayment payment(
    Invoice invoice, Transaction transaction, BigDecimal amount
  ) {
    return InvoicePayment.create(UUID.randomUUID(), invoice.id(),
      transaction.id(), transaction.accountId(), amount,
      transaction.bookingDate(), false, false, System.currentTimeMillis());
  }
}