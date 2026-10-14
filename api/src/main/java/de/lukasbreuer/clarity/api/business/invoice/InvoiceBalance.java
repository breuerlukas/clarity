package de.lukasbreuer.clarity.api.business.invoice;

import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * What of an invoice is paid and what is still open, given the links to
 * bookings. Links of other invoices and links the owner removed count for
 * nothing, and a settled invoice has nothing open any more.
 */
@RequiredArgsConstructor(staticName = "of")
final class InvoiceBalance {
  private static final BigDecimal TOLERANCE = new BigDecimal("0.01");

  private final Invoice invoice;
  private final List<InvoicePayment> payments;

  List<InvoicePayment> accepted() {
    return payments.stream()
      .filter(payment -> payment.invoiceId().equals(invoice.id()))
      .filter(payment -> !payment.rejected())
      .toList();
  }

  BigDecimal paid() {
    return accepted().stream()
      .map(InvoicePayment::amount)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  BigDecimal open() {
    if (invoice.settled()) {
      return BigDecimal.ZERO;
    }
    return invoice.grossAmount().subtract(paid()).max(BigDecimal.ZERO);
  }

  boolean isPaid() {
    return open().compareTo(TOLERANCE) < 0;
  }

  /**
   * The first that applies: paid, overdue, partly paid, announced, open.
   */
  InvoiceStatus status(LocalDate today) {
    if (isPaid()) {
      return InvoiceStatus.PAID;
    }
    if (invoice.dueDate() != null && invoice.dueDate().isBefore(today)) {
      return InvoiceStatus.OVERDUE;
    }
    return paid().signum() > 0 ? InvoiceStatus.PARTIAL : unpaidStatus();
  }

  private InvoiceStatus unpaidStatus() {
    return invoice.announcedAt() != null ? InvoiceStatus.ANNOUNCED :
      InvoiceStatus.OPEN;
  }
}