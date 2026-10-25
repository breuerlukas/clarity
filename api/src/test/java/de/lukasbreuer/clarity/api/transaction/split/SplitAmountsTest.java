package de.lukasbreuer.clarity.api.transaction.split;

import de.lukasbreuer.clarity.api.transaction.Transaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SplitAmountsTest {
  private static final Transaction PAYMENT = Transaction.create(
    UUID.randomUUID(), UUID.randomUUID(), "reference", LocalDate.now(),
    new BigDecimal("-23.47"), "EUR", "REWE", null, "", "GROCERIES", false);

  @Test
  void partsHaveToAddUpExactly() {
    assertTrue(SplitAmounts.of(PAYMENT).fit(amounts("-18.48", "-4.99")));
    assertFalse(SplitAmounts.of(PAYMENT).fit(amounts("-18.48", "-4.98")));
  }

  @Test
  void partsGoInTheDirectionOfTheTransaction() {
    assertFalse(SplitAmounts.of(PAYMENT).fit(amounts("-25.47", "2.00")));
  }

  @Test
  void oneSplitNeedsTwoPartsAndNoneUndoesIt() {
    assertFalse(SplitAmounts.of(PAYMENT).fit(amounts("-23.47")));
    assertTrue(SplitAmounts.of(PAYMENT).fit(List.of()));
  }

  @Test
  void partsHaveAtMostCents() {
    assertFalse(SplitAmounts.of(PAYMENT).fit(amounts("-18.475", "-4.995")));
  }

  private List<BigDecimal> amounts(String... values) {
    return List.of(values).stream().map(BigDecimal::new).toList();
  }
}