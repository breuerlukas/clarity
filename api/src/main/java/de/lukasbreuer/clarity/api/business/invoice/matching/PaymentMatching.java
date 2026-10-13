package de.lukasbreuer.clarity.api.business.invoice.matching;

import de.lukasbreuer.clarity.api.business.invoice.Invoice;
import de.lukasbreuer.clarity.api.business.invoice.InvoiceDirection;
import de.lukasbreuer.clarity.api.business.invoice.InvoicePayment;
import de.lukasbreuer.clarity.api.business.invoice.reading.Iban;
import de.lukasbreuer.clarity.api.transaction.Transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Links bookings to the invoices they pay. Money in pays outgoing invoices,
 * money out pays incoming ones. A booking whose reference names invoice
 * numbers pays those, oldest first, as far as it goes: several invoices in
 * one transfer, or part of one. If it falls short of a single named invoice
 * by at most three percent, that is taken as an early-payment discount
 * (Skonto) and the invoice is settled. Without a number, a booking is only
 * linked when it is exactly the open amount of one invoice and its IBAN or
 * name fits that invoice as well; everything else is left to suggestions.
 * Bookings marked as private withdrawals or deposits are never linked, and
 * a link the owner removed is not made again.
 */
public final class PaymentMatching {
  private static final BigDecimal TOLERANCE = new BigDecimal("0.01");
  private static final BigDecimal DISCOUNT_SHARE = new BigDecimal("0.97");
  private static final int EARLY_PAYMENT_DAYS = 7;
  private static final int SUGGESTION_DAYS = 30;
  private static final int MAXIMUM_SUGGESTIONS = 10;

  /**
   * @param links New links to store
   * @param settled Invoices paid with an early-payment discount
   */
  public record Result(List<InvoicePayment> links, List<Invoice> settled) {
  }

  public record Suggestion(Transaction transaction, Set<MatchReason> reasons) {
  }

  private final List<Invoice> invoices;
  private final List<InvoicePayment> payments;
  private final Map<UUID, BigDecimal> transactionShares = new HashMap<>();

  private PaymentMatching(
    List<Invoice> invoices, List<InvoicePayment> payments
  ) {
    this.invoices = invoices;
    this.payments = new ArrayList<>(payments);
    payments.stream().filter(payment -> !payment.rejected())
      .forEach(payment -> transactionShares.merge(payment.transactionId(),
        payment.amount(), BigDecimal::add));
  }

  /**
   * @param payments Every link of these invoices and transactions, rejected
   *                 ones included
   * @param excluded Transactions that are something else
   */
  public static Result match(
    List<Invoice> invoices, List<Transaction> transactions,
    List<InvoicePayment> payments, Set<UUID> excluded
  ) {
    var matching = new PaymentMatching(invoices, payments);
    var links = new ArrayList<InvoicePayment>();
    var settled = new ArrayList<Invoice>();
    transactions.stream()
      .filter(transaction -> !excluded.contains(transaction.id()))
      .sorted(Comparator.comparing(Transaction::bookingDate))
      .forEach(transaction -> matching.match(transaction, links, settled));
    return new Result(links, settled);
  }

  public static List<Suggestion> suggest(
    Invoice invoice, List<Transaction> transactions,
    List<InvoicePayment> payments, Set<UUID> excluded
  ) {
    var matching = new PaymentMatching(List.of(invoice), payments);
    var reference = invoice.dueDate() != null ? invoice.dueDate() :
      invoice.invoiceDate();
    return transactions.stream()
      .filter(transaction -> !excluded.contains(transaction.id()))
      .filter(transaction -> matching.fitsDirection(invoice, transaction))
      .filter(transaction -> !transaction.bookingDate().isBefore(
        invoice.invoiceDate().minusDays(SUGGESTION_DAYS)))
      .filter(transaction -> matching.remaining(transaction).signum() > 0)
      .filter(transaction -> !matching.isLinked(invoice, transaction, false))
      .map(transaction -> new Suggestion(transaction,
        matching.reasons(invoice, transaction)))
      .filter(suggestion -> !suggestion.reasons().isEmpty())
      .sorted(Comparator.<Suggestion>comparingInt(suggestion ->
          -suggestion.reasons().size())
        .thenComparingLong(suggestion -> Math.abs(ChronoUnit.DAYS.between(
          reference, suggestion.transaction().bookingDate()))))
      .limit(MAXIMUM_SUGGESTIONS)
      .toList();
  }

  /**
   * What is left of a booking after the invoices it already pays.
   */
  public static BigDecimal remaining(
    Transaction transaction, List<InvoicePayment> payments
  ) {
    return new PaymentMatching(List.of(), payments).remaining(transaction);
  }

  private void match(
    Transaction transaction, List<InvoicePayment> links, List<Invoice> settled
  ) {
    var candidates = invoices.stream()
      .filter(invoice -> fitsDirection(invoice, transaction))
      .filter(invoice -> invoice.currency().equalsIgnoreCase(
        transaction.currency()))
      .filter(invoice -> !transaction.bookingDate().isBefore(
        invoice.invoiceDate().minusDays(EARLY_PAYMENT_DAYS)))
      .filter(invoice -> !settled.contains(invoice))
      .filter(invoice -> invoice.openAmount(payments).signum() > 0)
      .filter(invoice -> !isLinked(invoice, transaction, true))
      .sorted(Comparator.comparing(Invoice::invoiceDate))
      .toList();
    var numbered = candidates.stream()
      .filter(invoice -> InvoiceNumber.appearsIn(invoice.number(),
        transaction.description()))
      .toList();
    if (!numbered.isEmpty()) {
      payNumbered(transaction, numbered, links, settled);
      return;
    }
    var exact = candidates.stream()
      .filter(invoice -> isExactly(invoice, transaction))
      .filter(invoice -> isFromIban(invoice, transaction) ||
        hasName(invoice, transaction))
      .toList();
    if (exact.size() == 1) {
      link(exact.getFirst(), transaction, remaining(transaction), links);
    }
  }

  private void payNumbered(
    Transaction transaction, List<Invoice> numbered,
    List<InvoicePayment> links, List<Invoice> settled
  ) {
    for (var invoice : numbered) {
      var remaining = remaining(transaction);
      if (remaining.signum() <= 0) {
        return;
      }
      var open = invoice.openAmount(payments);
      link(invoice, transaction, remaining.min(open), links);
      if (numbered.size() == 1 && remaining.compareTo(open) < 0 &&
        remaining.compareTo(open.multiply(DISCOUNT_SHARE)) >= 0) {
        invoice.settle(true);
        settled.add(invoice);
      }
    }
  }

  private void link(
    Invoice invoice, Transaction transaction, BigDecimal amount,
    List<InvoicePayment> links
  ) {
    var payment = InvoicePayment.create(UUID.randomUUID(), invoice.id(),
      transaction.id(), transaction.accountId(), amount,
      transaction.bookingDate(), true, false, System.currentTimeMillis());
    links.add(payment);
    payments.add(payment);
    transactionShares.merge(transaction.id(), amount, BigDecimal::add);
  }

  private BigDecimal remaining(Transaction transaction) {
    return amount(transaction).subtract(transactionShares.getOrDefault(
      transaction.id(), BigDecimal.ZERO)).max(BigDecimal.ZERO);
  }

  private BigDecimal amount(Transaction transaction) {
    return transaction.amount().abs().setScale(2, RoundingMode.HALF_UP);
  }

  private boolean fitsDirection(Invoice invoice, Transaction transaction) {
    return transaction.amount().signum() != 0 &&
      (transaction.amount().signum() > 0) ==
        (invoice.direction() == InvoiceDirection.OUTGOING);
  }

  /**
   * @param includeRejected Whether a link the owner removed counts
   */
  private boolean isLinked(
    Invoice invoice, Transaction transaction, boolean includeRejected
  ) {
    return payments.stream().anyMatch(payment ->
      payment.invoiceId().equals(invoice.id()) &&
        payment.transactionId().equals(transaction.id()) &&
        (includeRejected || !payment.rejected()));
  }

  private Set<MatchReason> reasons(Invoice invoice, Transaction transaction) {
    var reasons = EnumSet.noneOf(MatchReason.class);
    if (InvoiceNumber.appearsIn(invoice.number(), transaction.description())) {
      reasons.add(MatchReason.NUMBER);
    }
    if (isExactly(invoice, transaction)) {
      reasons.add(MatchReason.AMOUNT);
    }
    if (isFromIban(invoice, transaction)) {
      reasons.add(MatchReason.IBAN);
    }
    if (hasName(invoice, transaction)) {
      reasons.add(MatchReason.NAME);
    }
    return reasons;
  }

  private boolean isExactly(Invoice invoice, Transaction transaction) {
    return remaining(transaction).subtract(invoice.openAmount(payments))
      .abs().compareTo(TOLERANCE) < 0;
  }

  private boolean isFromIban(Invoice invoice, Transaction transaction) {
    return invoice.direction() == InvoiceDirection.INCOMING &&
      invoice.iban() != null && transaction.counterpartyIban() != null &&
      invoice.iban().equals(Iban.normalize(transaction.counterpartyIban()));
  }

  /**
   * The other side of the booking is the customer for an outgoing invoice
   * and the issuer for an incoming one.
   */
  private boolean hasName(Invoice invoice, Transaction transaction) {
    var party = invoice.direction() == InvoiceDirection.OUTGOING ?
      invoice.recipient() : invoice.issuer();
    return CompanyName.isSimilar(party, transaction.counterparty());
  }
}
