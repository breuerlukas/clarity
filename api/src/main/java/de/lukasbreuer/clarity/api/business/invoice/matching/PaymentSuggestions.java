package de.lukasbreuer.clarity.api.business.invoice.matching;

import de.lukasbreuer.clarity.api.business.invoice.Invoice;
import de.lukasbreuer.clarity.api.transaction.Transaction;
import lombok.RequiredArgsConstructor;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Bookings the owner may link to an invoice by hand: those of the right
 * direction from a month before the invoice on, with something left that
 * is not linked yet, and at least one reason to fit. The most reasons come
 * first, then the closest to the due date.
 */
@RequiredArgsConstructor(staticName = "of")
final class PaymentSuggestions {
  private static final int SUGGESTION_DAYS = 30;
  private static final int MAXIMUM_SUGGESTIONS = 10;

  private final Invoice invoice;
  private final PaymentLedger ledger;

  List<PaymentSuggestion> find(
    List<Transaction> transactions, Set<UUID> excluded
  ) {
    return transactions.stream()
      .filter(transaction -> !excluded.contains(transaction.id()))
      .map(transaction -> PaymentFit.of(invoice, transaction, ledger))
      .filter(this::isSuggestable)
      .map(fit -> new PaymentSuggestion(fit.transaction(), fit.reasons()))
      .filter(suggestion -> !suggestion.reasons().isEmpty())
      .sorted(order())
      .limit(MAXIMUM_SUGGESTIONS)
      .toList();
  }

  private boolean isSuggestable(PaymentFit fit) {
    return fit.fitsDirection() && fit.isBookedSince(SUGGESTION_DAYS) &&
      ledger.remaining(fit.transaction()).signum() > 0 &&
      !ledger.isLinked(invoice, fit.transaction(), false);
  }

  private Comparator<PaymentSuggestion> order() {
    var reference = invoice.dueDate() != null ? invoice.dueDate() :
      invoice.invoiceDate();
    return Comparator.<PaymentSuggestion>comparingInt(suggestion ->
        -suggestion.reasons().size())
      .thenComparingLong(suggestion -> suggestion.daysFrom(reference));
  }
}