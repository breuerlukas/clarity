package de.lukasbreuer.clarity.api.business.invoice.matching;

import de.lukasbreuer.clarity.api.business.invoice.Invoice;
import de.lukasbreuer.clarity.api.business.invoice.InvoiceDirection;
import de.lukasbreuer.clarity.api.business.invoice.reading.Iban;
import de.lukasbreuer.clarity.api.transaction.Transaction;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * How well a booking fits an invoice: whether its direction and date can
 * fit at all, and which {@link MatchReason}s speak for it.
 */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(staticName = "of")
final class PaymentFit {
  private static final BigDecimal TOLERANCE = new BigDecimal("0.01");

  private final Invoice invoice;
  private final Transaction transaction;
  private final PaymentLedger ledger;

  /**
   * Money in pays outgoing invoices, money out pays incoming ones.
   */
  boolean fitsDirection() {
    var sign = transaction.amount().signum();
    return sign != 0 &&
      (sign > 0) == (invoice.direction() == InvoiceDirection.OUTGOING);
  }

  boolean hasSameCurrency() {
    return invoice.currency().equalsIgnoreCase(transaction.currency());
  }

  /**
   * @param daysBeforeInvoice How long before the invoice date a booking may
   *                          already pay it
   */
  boolean isBookedSince(int daysBeforeInvoice) {
    return !transaction.bookingDate().isBefore(
      invoice.invoiceDate().minusDays(daysBeforeInvoice));
  }

  boolean namesNumber() {
    return InvoiceNumber.of(invoice.number())
      .appearsIn(transaction.description());
  }

  /**
   * Without a number, a booking only pays an invoice when it is exactly the
   * open amount and its IBAN or name fits as well.
   */
  boolean fitsWithoutNumber() {
    return isExactAmount() && (isFromIban() || hasName());
  }

  Set<MatchReason> reasons() {
    return Arrays.stream(MatchReason.values())
      .filter(this::applies)
      .collect(Collectors.toCollection(() ->
        EnumSet.noneOf(MatchReason.class)));
  }

  private boolean applies(MatchReason reason) {
    return switch (reason) {
      case NUMBER -> namesNumber();
      case AMOUNT -> isExactAmount();
      case IBAN -> isFromIban();
      case NAME -> hasName();
    };
  }

  private boolean isExactAmount() {
    return ledger.remaining(transaction)
      .subtract(ledger.openAmount(invoice)).abs().compareTo(TOLERANCE) < 0;
  }

  private boolean isFromIban() {
    return invoice.direction() == InvoiceDirection.INCOMING &&
      invoice.iban() != null && transaction.counterpartyIban() != null &&
      invoice.iban().equals(Iban.of(transaction.counterpartyIban())
        .normalized());
  }

  /**
   * The other side of the booking is the customer for an outgoing invoice
   * and the issuer for an incoming one.
   */
  private boolean hasName() {
    var party = invoice.direction() == InvoiceDirection.OUTGOING ?
      invoice.recipient() : invoice.issuer();
    return CompanyName.of(party).isSimilarTo(transaction.counterparty());
  }
}