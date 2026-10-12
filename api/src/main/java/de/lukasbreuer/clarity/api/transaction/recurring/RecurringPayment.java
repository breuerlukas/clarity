package de.lukasbreuer.clarity.api.transaction.recurring;

import de.lukasbreuer.clarity.api.transaction.Category;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;

/**
 * A detected fixed cost. The amount is what the last booking cost, as a
 * positive number.
 */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(staticName = "create")
public final class RecurringPayment {
  private final String counterparty;
  private final Category category;
  private final BigDecimal amount;
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

  public Map<String, Object> information() {
    return Map.of("counterparty", counterparty,
      "category", category.name(),
      "amount", amount,
      "currency", currency,
      "cycle", cycle.name(),
      "last_date", lastDate.toString(),
      "next_date", nextDate().toString(),
      "monthly_amount", monthlyAmount());
  }
}