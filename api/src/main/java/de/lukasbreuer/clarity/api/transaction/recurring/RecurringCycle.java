package de.lukasbreuer.clarity.api.transaction.recurring;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Arrays;
import java.util.Optional;

/**
 * The rhythms a fixed cost can have, with how far a booking may drift
 * (weekends and bank holidays shift debits by a few days) and how many
 * bookings it takes before a rhythm counts as recurring.
 */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum RecurringCycle {
  WEEKLY(6, 8, 7, 4, 52.0 / 12),
  MONTHLY(26, 35, 30, 3, 1),
  QUARTERLY(84, 98, 91, 2, 1.0 / 3),
  YEARLY(350, 380, 365, 2, 1.0 / 12);

  private final int minimumDays;
  private final int maximumDays;
  private final int typicalDays;
  private final int minimumOccurrences;
  private final double perMonth;

  public boolean fits(long days) {
    return days >= minimumDays && days <= maximumDays;
  }

  public static Optional<RecurringCycle> find(long days) {
    return Arrays.stream(values()).filter(cycle -> cycle.fits(days))
      .findFirst();
  }
}