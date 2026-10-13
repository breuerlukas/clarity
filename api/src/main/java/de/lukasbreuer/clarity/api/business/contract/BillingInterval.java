package de.lukasbreuer.clarity.api.business.contract;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Arrays;
import java.util.Optional;

/**
 * How often a customer is billed, and so how many months one invoice covers.
 */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum BillingInterval {
  MONTHLY(1),
  QUARTERLY(3),
  HALF_YEARLY(6),
  YEARLY(12);

  private final int months;

  public static Optional<BillingInterval> find(String name) {
    return Arrays.stream(values())
      .filter(interval -> interval.name().equals(name))
      .findFirst();
  }
}
