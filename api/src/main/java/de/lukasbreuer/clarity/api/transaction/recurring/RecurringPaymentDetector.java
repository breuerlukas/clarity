package de.lukasbreuer.clarity.api.transaction.recurring;

import de.lukasbreuer.clarity.api.transaction.Transaction;
import de.lukasbreuer.clarity.api.transaction.category.Category;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Finds fixed costs in the booking history: debits to the same counterparty
 * whose most recent bookings follow one rhythm, with amounts within 15% of
 * the latest one. Groceries at the same supermarket fail the amount check.
 * One that stopped stays in the list as missing, then ended, and drops out
 * a cycle after it ended.
 */
@RequiredArgsConstructor(staticName = "create")
public final class RecurringPaymentDetector {
  private static final BigDecimal AMOUNT_TOLERANCE = new BigDecimal("0.15");
  /**
   * A payment reference after the merchant's name, as card payments carry
   * one that changes every month ("SPOTIFY*P47BDD5092",
   * "Spotify P445538918"): a word of at least five letters and digits, with
   * a digit in it, after a star or a space.
   */
  private static final Pattern REFERENCE =
    Pattern.compile("[*\\s]+(?=[a-z]*\\d)[a-z0-9]{5,}\\b");

  private final LocalDate today;

  public List<RecurringPayment> detect(List<Transaction> transactions) {
    return transactions.stream()
      .filter(Transaction::isExpense)
      .filter(transaction -> !Category.TRANSFER.equals(transaction.category()))
      .collect(Collectors.groupingBy(this::groupKey))
      .entrySet().stream()
      .map(group -> detectInGroup(group.getKey(), group.getValue()))
      .flatMap(Optional::stream)
      .sorted(Comparator.comparing(RecurringPayment::monthlyAmount).reversed())
      .toList();
  }

  private String groupKey(Transaction transaction) {
    var source = transaction.counterparty().isBlank() ?
      transaction.description() : transaction.counterparty();
    var name = source.trim().toLowerCase(Locale.ROOT);
    return REFERENCE.matcher(name).replaceAll("").replaceAll("\\s+", " ")
      .trim();
  }

  private Optional<RecurringPayment> detectInGroup(
    String key, List<Transaction> group
  ) {
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
      .filter(cycle -> isRecent(latest.bookingDate(), cycle))
      .map(cycle -> RecurringPayment.create(key, latest.counterparty(),
        latest.category(), latest.amount().abs(),
        sorted.get(sorted.size() - 2).amount().abs(), latest.currency(), cycle,
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

  private boolean isRecent(LocalDate lastDate, RecurringCycle cycle) {
    return !today.isAfter(cycle.overdueAfter(lastDate)
      .plusDays(2L * cycle.maximumDays()));
  }

  private long daysBetween(List<Transaction> sorted, int index) {
    return ChronoUnit.DAYS.between(sorted.get(index - 1).bookingDate(),
      sorted.get(index).bookingDate());
  }
}