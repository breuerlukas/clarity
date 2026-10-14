package de.lukasbreuer.clarity.api.business.invoice.matching;

import de.lukasbreuer.clarity.api.business.invoice.Invoice;
import de.lukasbreuer.clarity.api.business.invoice.InvoicePayment;
import de.lukasbreuer.clarity.api.transaction.Transaction;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
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
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class PaymentMatching {
  private static final BigDecimal DISCOUNT_SHARE = new BigDecimal("0.97");
  private static final int EARLY_PAYMENT_DAYS = 7;

  /**
   * @param payments Every link of these invoices and the bookings, rejected
   *                 ones included
   */
  public static PaymentMatching create(
    List<Invoice> invoices, List<InvoicePayment> payments
  ) {
    return new PaymentMatching(invoices, PaymentLedger.create(payments));
  }

  private final List<Invoice> invoices;
  private final PaymentLedger ledger;

  /**
   * @param excluded Transactions that are something else
   */
  public MatchResult match(List<Transaction> transactions, Set<UUID> excluded) {
    var result = MatchResult.empty();
    transactions.stream()
      .filter(transaction -> !excluded.contains(transaction.id()))
      .sorted(Comparator.comparing(Transaction::bookingDate))
      .forEach(transaction -> match(transaction, result));
    return result;
  }

  public List<PaymentSuggestion> suggest(
    Invoice invoice, List<Transaction> transactions, Set<UUID> excluded
  ) {
    return PaymentSuggestions.of(invoice, ledger)
      .find(transactions, excluded);
  }

  /**
   * What is left of a booking after the invoices it already pays.
   */
  public BigDecimal remaining(Transaction transaction) {
    return ledger.remaining(transaction);
  }

  private void match(Transaction transaction, MatchResult result) {
    var candidates = candidates(transaction, result);
    var numbered = candidates.stream().filter(PaymentFit::namesNumber)
      .toList();
    if (!numbered.isEmpty()) {
      numbered.forEach(fit -> pay(fit, numbered.size() == 1, result));
      return;
    }
    var exact = candidates.stream().filter(PaymentFit::fitsWithoutNumber)
      .toList();
    if (exact.size() == 1) {
      link(exact.getFirst(), remaining(transaction), result);
    }
  }

  private List<PaymentFit> candidates(
    Transaction transaction, MatchResult result
  ) {
    return invoices.stream()
      .filter(invoice -> !result.settled().contains(invoice))
      .map(invoice -> PaymentFit.of(invoice, transaction, ledger))
      .filter(this::isCandidate)
      .sorted(Comparator.comparing(fit -> fit.invoice().invoiceDate()))
      .toList();
  }

  private boolean isCandidate(PaymentFit fit) {
    return fit.fitsDirection() && fit.hasSameCurrency() &&
      fit.isBookedSince(EARLY_PAYMENT_DAYS) &&
      ledger.openAmount(fit.invoice()).signum() > 0 &&
      !ledger.isLinked(fit.invoice(), fit.transaction(), true);
  }

  /**
   * Pays a named invoice with what is left of the booking. A single named
   * invoice that falls short by an early-payment discount is settled.
   */
  private void pay(PaymentFit fit, boolean onlyInvoice, MatchResult result) {
    var remaining = remaining(fit.transaction());
    if (remaining.signum() <= 0) {
      return;
    }
    var open = ledger.openAmount(fit.invoice());
    link(fit, remaining.min(open), result);
    if (onlyInvoice && isDiscounted(remaining, open)) {
      fit.invoice().settle(true);
      result.settled().add(fit.invoice());
    }
  }

  private boolean isDiscounted(BigDecimal paid, BigDecimal open) {
    return paid.compareTo(open) < 0 &&
      paid.compareTo(open.multiply(DISCOUNT_SHARE)) >= 0;
  }

  private void link(PaymentFit fit, BigDecimal amount, MatchResult result) {
    result.links().add(ledger.link(fit.invoice(), fit.transaction(), amount));
  }
}