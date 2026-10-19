package de.lukasbreuer.clarity.api.business.invoice.reading;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Labels an invoice or a receipt prints in front of an amount, such as
 * "Gesamtbetrag", in the order they are trusted.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class AmountLabels {
  static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
  static final String AMOUNT =
    "(-?(?:\\d{1,3}(?:[.,' ]\\d{3})+|\\d+)(?:[.,]\\d{1,2})?)";
  static final String GAP = "[^\\n\\d]{0,40}?";

  /**
   * @param labels In the order they are trusted, each followed by its amount
   */
  public static AmountLabels of(LabelPhrases labels) {
    return new AmountLabels(labels.each().stream()
      .map(label -> Pattern.compile("\\b" + label + "\\b" + GAP + AMOUNT,
        FLAGS))
      .toList());
  }

  private final List<Pattern> patterns;

  /**
   * The amount after the first label that appears, in the order of the list;
   * for a label printed several times the largest amount, since subtotals
   * come before the total.
   */
  public Optional<BigDecimal> firstIn(String text) {
    return patterns.stream()
      .map(pattern -> amounts(pattern, text))
      .filter(amounts -> !amounts.isEmpty())
      .findFirst()
      .flatMap(amounts -> amounts.stream().max(Comparator.naturalOrder()));
  }

  private List<BigDecimal> amounts(Pattern pattern, String text) {
    return pattern.matcher(text).results()
      .map(result -> AmountText.of(result.group(1)).value())
      .flatMap(Optional::stream)
      .toList();
  }
}