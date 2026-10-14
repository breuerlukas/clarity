package de.lukasbreuer.clarity.api.business.invoice.reading;

import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * An amount as invoices print it: German "1.234,56", English "1,234.56", or
 * plain "1234.56". The last separator followed by exactly one or two digits
 * is the decimal one.
 */
@RequiredArgsConstructor(staticName = "of")
public final class AmountText {
  private final String text;

  public Optional<BigDecimal> value() {
    var digits = text.replaceAll("[^0-9,.\\-]", "");
    if (!digits.matches(".*[0-9].*")) {
      return Optional.empty();
    }
    return parse(withDecimalPoint(digits));
  }

  private String withDecimalPoint(String digits) {
    var separator = Math.max(digits.lastIndexOf(','), digits.lastIndexOf('.'));
    var decimals = separator < 0 ? 0 : digits.length() - separator - 1;
    if (decimals < 1 || decimals > 2) {
      return digits.replaceAll("[,.]", "");
    }
    return digits.substring(0, separator).replaceAll("[,.]", "") + "." +
      digits.substring(separator + 1);
  }

  private Optional<BigDecimal> parse(String number) {
    try {
      return Optional.of(new BigDecimal(number)
        .setScale(2, RoundingMode.HALF_UP));
    } catch (NumberFormatException exception) {
      return Optional.empty();
    }
  }
}