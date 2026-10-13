package de.lukasbreuer.clarity.api.business.invoice.reading;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Reads amounts as invoices print them: German "1.234,56", English
 * "1,234.56", or plain "1234.56". The last separator followed by exactly one
 * or two digits is the decimal one.
 */
public final class Amounts {
  private Amounts() {
  }

  public static Optional<BigDecimal> parse(String text) {
    var value = text.replaceAll("[^0-9,.\\-]", "");
    if (value.isEmpty() || !value.matches(".*[0-9].*")) {
      return Optional.empty();
    }
    var decimal = Math.max(value.lastIndexOf(','), value.lastIndexOf('.'));
    var digitsAfter = decimal < 0 ? 0 : value.length() - decimal - 1;
    String normalized;
    if (decimal >= 0 && digitsAfter >= 1 && digitsAfter <= 2) {
      normalized = value.substring(0, decimal).replaceAll("[,.]", "") + "." +
        value.substring(decimal + 1);
    } else {
      normalized = value.replaceAll("[,.]", "");
    }
    try {
      return Optional.of(new BigDecimal(normalized)
        .setScale(2, RoundingMode.HALF_UP));
    } catch (NumberFormatException exception) {
      return Optional.empty();
    }
  }
}
