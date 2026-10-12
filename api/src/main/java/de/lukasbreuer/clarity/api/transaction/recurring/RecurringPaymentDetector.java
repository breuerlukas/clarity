package de.lukasbreuer.clarity.api.transaction.recurring;

import de.lukasbreuer.clarity.api.transaction.Category;
import de.lukasbreuer.clarity.api.transaction.Transaction;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Finds fixed costs in the booking history: debits to the same counterparty
 * whose most recent bookings follow one rhythm, with amounts within 15% of
 * the latest one, and that have not stopped. Groceries at the same
 * supermarket fail the amount check; a cancelled subscription fails the
 * "not stopped" check once its next booking is overdue.
 */
@RequiredArgsConstructor(staticName = "create")
public final class RecurringPaymentDetector {
  private static final BigDecimal AMOUNT_TOLERANCE = new BigDecimal("0.15");
  private static final int GRACE_DAYS = 7;

  private final LocalDate today;

  public List<RecurringPayment> detect(List<Transaction> transactions) {
    return transactions.stream()
      .filter(Transaction::isExpense)
      .filter(transaction -> transaction.category() != Category.TRANSFER)
      .collect(Collectors.groupingBy(this::groupKey))
      .values().stream()
      .map(this::detectInGroup)
      .flatMap(Optional::stream)
      .sorted(Comparator.comparing(RecurringPayment::monthlyAmount).reversed())
      .toList();
  }

  private String groupKey(Transaction transaction) {
    var source = transaction.counterparty().isBlank() ?
      transaction.description() : transaction.counterparty();
    return source.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
  }

  private Optional<RecurringPayment> detectInGroup(List<Transaction> group) {
    var sorted = group.stream()
      .sorted(Comparator.comparing(Transaction::bookingDate)).toList();
    if (sorted.size() < 2) {
      return Optional.empty();
    }
    var latest = sorted.getLast();
    return RecurringCycle.find(daysBetween(sorted, sorted.size() - 1))
      .filter(cycle -> sorted.size() >= cycle.minimumOccurrences())
      .filter(cycle -> isRegular(recent(sorted, cycle), cycle))
      .filter(cycle -> isStable(recent(sorted, cycle), latest.amount()))
      .filter(cycle -> isActive(latest.bookingDate(), cycle))
      .map(cycle -> RecurringPayment.create(latest.counterparty(),
        latest.category(), latest.amount().abs(), latest.currency(), cycle,
        latest.bookingDate()));
  }

  private List<Transaction> recent(
    List<Transaction> sorted, RecurringCycle cycle
  ) {
    return sorted.subList(sorted.size() - cycle.minimumOccurrences(),
      sorted.size());
  }

  private boolean isRegular(List<Transaction> recent, RecurringCycle cycle) {
    return IntStream.range(1, recent.size())
      .allMatch(index -> cycle.fits(daysBetween(recent, index)));
  }

  private boolean isStable(List<Transaction> recent, BigDecimal latestAmount) {
    var allowed = latestAmount.abs().multiply(AMOUNT_TOLERANCE);
    return recent.stream().allMatch(transaction -> transaction.amount()
      .subtract(latestAmount).abs().compareTo(allowed) <= 0);
  }

  private boolean isActive(LocalDate lastDate, RecurringCycle cycle) {
    return !today.isAfter(lastDate.plusDays(cycle.maximumDays() + GRACE_DAYS));
  }

  private long daysBetween(List<Transaction> sorted, int index) {
    return ChronoUnit.DAYS.between(sorted.get(index - 1).bookingDate(),
      sorted.get(index).bookingDate());
  }
}