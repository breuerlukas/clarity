package de.lukasbreuer.clarity.api.transaction.receipt;

import de.lukasbreuer.clarity.api.business.invoice.reading.AmountText;
import de.lukasbreuer.clarity.api.business.invoice.reading.PrintedDate;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The text of a photographed or scanned receipt, and what can be guessed
 * from it: the merchant is the first line with words in it, the date the
 * first one printed, the total the amount after a total label, and the
 * items the lines above the total that end in an amount. Quantity lines
 * ("2 x 0,99") and lines about tax or the payment are skipped.
 */
@RequiredArgsConstructor(staticName = "of")
public final class ReceiptText {
  private static final Pattern ITEM = Pattern.compile(
    "^(.*?\\p{L}.*?)\\s+(-?\\d{1,5}[.,]\\d{2})(?:\\s*[A-Z*]{1,2})?$");
  private static final Pattern QUANTITY = Pattern.compile(
    "\\d\\s*(?:stk\\.?\\s*)?[x×*]\\s*\\d", Pattern.CASE_INSENSITIVE);
  private static final Pattern DATE = Pattern.compile(PrintedDate.PATTERN);
  private static final int MINIMUM_MERCHANT_LETTERS = 3;
  private static final int MAXIMUM_NAME_LENGTH = 120;
  private static final int MAXIMUM_ITEMS = 100;

  private final ReceiptVocabulary vocabulary;
  private final String text;

  public RecognisedReceipt recognise() {
    return new RecognisedReceipt(merchant(), date().orElse(null),
      total().orElse(null), items());
  }

  String merchant() {
    return lines()
      .filter(line -> letters(line) >= MINIMUM_MERCHANT_LETTERS)
      .filter(line -> !vocabulary.isOtherLine(line))
      .findFirst().map(this::shortened).orElse("");
  }

  Optional<LocalDate> date() {
    return DATE.matcher(text).results().findFirst()
      .flatMap(result -> PrintedDate.of(result.group(1), Map.of()).date());
  }

  Optional<BigDecimal> total() {
    return vocabulary.total(text).filter(total -> total.signum() > 0);
  }

  List<ReceiptItem> items() {
    return lines().takeWhile(line -> !vocabulary.isTotalLine(line))
      .filter(this::mayBeItem)
      .map(ITEM::matcher)
      .filter(Matcher::matches)
      .map(this::item)
      .flatMap(Optional::stream)
      .limit(MAXIMUM_ITEMS)
      .toList();
  }

  private boolean mayBeItem(String line) {
    return !vocabulary.isOtherLine(line) && !QUANTITY.matcher(line).find() &&
      !DATE.matcher(line).find();
  }

  private Optional<ReceiptItem> item(Matcher line) {
    return AmountText.of(line.group(2)).value()
      .map(amount -> new ReceiptItem(shortened(line.group(1)), amount));
  }

  private Stream<String> lines() {
    return text.lines().map(String::strip).filter(line -> !line.isEmpty());
  }

  private long letters(String line) {
    return line.chars().filter(Character::isLetter).count();
  }

  private String shortened(String line) {
    return line.substring(0, Math.min(line.length(), MAXIMUM_NAME_LENGTH));
  }
}