package de.lukasbreuer.clarity.api.business.invoice.reading;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AmountTextTest {
  @Test
  void readsGermanAndEnglishSeparators() {
    assertEquals(new BigDecimal("1234.56"), AmountText.of("1.234,56 €")
      .value().orElseThrow());
    assertEquals(new BigDecimal("1234.56"), AmountText.of("1,234.56")
      .value().orElseThrow());
  }

  @Test
  void takesOnlyOneOrTwoDigitsAsDecimals() {
    assertEquals(new BigDecimal("1234.00"), AmountText.of("1.234")
      .value().orElseThrow());
    assertEquals(new BigDecimal("12.50"), AmountText.of("12,5")
      .value().orElseThrow());
  }

  @Test
  void ignoresTextWithoutDigits() {
    assertTrue(AmountText.of("EUR").value().isEmpty());
  }
}