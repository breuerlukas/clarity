package de.lukasbreuer.clarity.api.transaction.receipt;

import de.lukasbreuer.clarity.api.business.invoice.reading.AmountLabels;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * What receipts print in German and English: the labels in front of the
 * total, in the order they are trusted, and the words of lines that are no
 * item, such as the tax, the payment or the change. The words live in
 * {@code receipt-vocabulary.json}; a missing one is added there.
 */
@RequiredArgsConstructor(staticName = "create")
public final class ReceiptVocabulary {
  private final AmountLabels totals;
  private final Pattern totalLine;
  private final Pattern otherLine;

  Optional<BigDecimal> total(String text) {
    return totals.firstIn(text);
  }

  boolean isTotalLine(String line) {
    return totalLine.matcher(line).find();
  }

  boolean isOtherLine(String line) {
    return otherLine.matcher(line).find();
  }
}