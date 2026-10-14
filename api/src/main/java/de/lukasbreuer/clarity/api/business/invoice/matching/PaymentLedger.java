package de.lukasbreuer.clarity.api.business.invoice.matching;

import de.lukasbreuer.clarity.api.business.invoice.Invoice;
import de.lukasbreuer.clarity.api.business.invoice.InvoicePayment;
import de.lukasbreuer.clarity.api.transaction.Transaction;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The links between invoices and bookings during one matching run: the
 * stored ones plus those the run makes. It knows what is still open of an
 * invoice and what is left of a booking for further invoices.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
final class PaymentLedger {
  /**
   * @param payments Every link of the invoices and bookings, rejected ones
   *                 included
   */
  static PaymentLedger create(List<InvoicePayment> payments) {
    var shares = payments.stream()
      .filter(payment -> !payment.rejected())
      .collect(Collectors.toMap(InvoicePayment::transactionId,
        InvoicePayment::amount, BigDecimal::add, HashMap::new));
    return new PaymentLedger(new ArrayList<>(payments), shares);
  }

  private final List<InvoicePayment> payments;
  private final Map<UUID, BigDecimal> transactionShares;

  BigDecimal openAmount(Invoice invoice) {
    return invoice.openAmount(payments);
  }

  /**
   * What is left of a booking after the invoices it already pays.
   */
  BigDecimal remaining(Transaction transaction) {
    var amount = transaction.amount().abs().setScale(2, RoundingMode.HALF_UP);
    return amount.subtract(transactionShares.getOrDefault(transaction.id(),
      BigDecimal.ZERO)).max(BigDecimal.ZERO);
  }

  /**
   * @param includeRejected Whether a link the owner removed counts
   */
  boolean isLinked(
    Invoice invoice, Transaction transaction, boolean includeRejected
  ) {
    return payments.stream().anyMatch(payment ->
      payment.invoiceId().equals(invoice.id()) &&
        payment.transactionId().equals(transaction.id()) &&
        (includeRejected || !payment.rejected()));
  }

  InvoicePayment link(
    Invoice invoice, Transaction transaction, BigDecimal amount
  ) {
    var payment = InvoicePayment.create(UUID.randomUUID(), invoice.id(),
      transaction.id(), transaction.accountId(), amount,
      transaction.bookingDate(), true, false, System.currentTimeMillis());
    payments.add(payment);
    transactionShares.merge(transaction.id(), amount, BigDecimal::add);
    return payment;
  }
}