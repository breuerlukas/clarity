package de.lukasbreuer.clarity.api.transaction.recurring;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A detected fixed cost. The amount is what the last booking cost, the
 * previous amount what the one before cost, both as positive numbers; a
 * difference between the two is a price change. The key is what its
 * bookings were grouped by, so the owner's decisions find it again.
 */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(staticName = "create")
public final class RecurringPayment {
  private final String key;
  private final String counterparty;
  private final String category;
  private final BigDecimal amount;
  private final BigDecimal previousAmount;
  private final String currency;
  private final RecurringCycle cycle;
  private final LocalDate lastDate;

  public LocalDate nextDate() {
    return switch (cycle) {
      case WEEKLY -> lastDate.plusWeeks(1);
      case MONTHLY -> lastDate.plusMonths(1);
      case QUARTERLY -> lastDate.plusMonths(3);
      case YEARLY -> lastDate.plusYears(1);
    };
  }

  public BigDecimal monthlyAmount() {
    return amount.multiply(BigDecimal.valueOf(cycle.perMonth()))
      .setScale(2, RoundingMode.HALF_UP);
  }

  /**
   * Overdue by more than the cycle allows is missing, a full cycle later
   * ended. The owner's word comes first: cancelled ends it once nothing is
   * due before the end date, "not ended" keeps it running until it books
   * again.
   */
  public RecurringStatus status(
    LocalDate today, Optional<RecurringSetting> setting
  ) {
    if (setting.filter(entry -> entry.endsBefore(nextDate())).isPresent()) {
      return RecurringStatus.ENDED;
    }
    if (setting.filter(entry -> entry.dismisses(lastDate)).isPresent()) {
      return RecurringStatus.ACTIVE;
    }
    return detectedStatus(today);
  }

  private RecurringStatus detectedStatus(LocalDate today) {
    var overdueAfter = cycle.overdueAfter(lastDate);
    if (!today.isAfter(overdueAfter)) {
      return RecurringStatus.ACTIVE;
    }
    return today.isAfter(overdueAfter.plusDays(cycle.maximumDays())) ?
      RecurringStatus.ENDED : RecurringStatus.MISSING;
  }

  public Map<String, Object> information(
    LocalDate today, Optional<RecurringSetting> setting
  ) {
    var information = new HashMap<>(information());
    information.put("status", status(today, setting).name());
    information.put("cancelled_on", setting.map(RecurringSetting::cancelledOn)
      .map(LocalDate::toString).orElse(null));
    information.put("dismissed_last_date", setting
      .map(RecurringSetting::dismissedLastDate).map(LocalDate::toString)
      .orElse(null));
    information.put("ignored", setting.filter(RecurringSetting::isIgnored)
      .isPresent());
    information.put("notice_days", setting.map(RecurringSetting::noticeDays)
      .orElse(null));
    information.put("cancel_by", setting.flatMap(entry ->
      entry.cancelBy(nextDate())).map(LocalDate::toString).orElse(null));
    return information;
  }

  private Map<String, Object> information() {
    return Map.of("key", key,
      "counterparty", counterparty,
      "category", category,
      "amount", amount,
      "previous_amount", previousAmount,
      "currency", currency,
      "cycle", cycle.name(),
      "last_date", lastDate.toString(),
      "next_date", nextDate().toString(),
      "monthly_amount", monthlyAmount());
  }
}