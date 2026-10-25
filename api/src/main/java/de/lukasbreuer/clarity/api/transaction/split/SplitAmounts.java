package de.lukasbreuer.clarity.api.transaction.split;

import de.lukasbreuer.clarity.api.transaction.Transaction;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Whether amounts can split a transaction: at least two parts with cents at
 * most, each in the transaction's direction, that add up to it exactly.
 * Anything else would let a statistic count money that never moved. No
 * parts at all undo a split.
 */
@RequiredArgsConstructor(staticName = "of")
public final class SplitAmounts {
  private final Transaction transaction;

  public boolean fit(List<BigDecimal> amounts) {
    if (amounts.isEmpty()) {
      return true;
    }
    return amounts.size() >= 2 && amounts.stream().allMatch(this::isPart) &&
      sum(amounts).compareTo(transaction.amount()) == 0;
  }

  private boolean isPart(BigDecimal amount) {
    return amount.signum() == transaction.amount().signum() &&
      amount.stripTrailingZeros().scale() <= 2;
  }

  private BigDecimal sum(List<BigDecimal> amounts) {
    return amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}