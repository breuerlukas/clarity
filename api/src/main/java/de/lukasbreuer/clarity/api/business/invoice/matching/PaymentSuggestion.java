package de.lukasbreuer.clarity.api.business.invoice.matching;

import de.lukasbreuer.clarity.api.transaction.Transaction;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;

/**
 * A booking that may pay an invoice, and why.
 */
public record PaymentSuggestion(
  Transaction transaction, Set<MatchReason> reasons
) {
  long daysFrom(LocalDate date) {
    return Math.abs(ChronoUnit.DAYS.between(date, transaction.bookingDate()));
  }
}