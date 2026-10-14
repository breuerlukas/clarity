package de.lukasbreuer.clarity.api.business.invoice.reading;

import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * The text of a PDF or scan that may be an invoice, in German or English,
 * and the invoice fields that can be guessed from it with the words of the
 * {@link InvoiceVocabulary}.
 */
@RequiredArgsConstructor(staticName = "of")
final class InvoiceText {
  private static final Pattern ANY_DATE =
    Pattern.compile(PrintedDate.PATTERN, AmountLabels.FLAGS);

  private final InvoiceVocabulary vocabulary;
  private final String text;

  /**
   * It names an invoice number, or has a heading that says invoice and
   * none that says it is something else, such as an offer or a credit note.
   */
  boolean isInvoice() {
    return number().isPresent() ||
      (vocabulary.heading().matcher(text).find() &&
        !vocabulary.otherDocument().matcher(text).find());
  }

  Optional<String> number() {
    return find(vocabulary.number());
  }

  Optional<BigDecimal> gross() {
    return vocabulary.gross().firstIn(text);
  }

  Optional<BigDecimal> net(BigDecimal gross) {
    return vocabulary.net().firstIn(text)
      .filter(net -> net.compareTo(gross) <= 0);
  }

  Optional<BigDecimal> tax(BigDecimal gross) {
    return find(vocabulary.tax()).flatMap(tax -> AmountText.of(tax).value())
      .filter(tax -> tax.compareTo(gross) < 0);
  }

  /**
   * The labelled invoice date, otherwise the first date in the text.
   */
  Optional<LocalDate> invoiceDate() {
    return find(vocabulary.invoiceDate()).flatMap(this::date)
      .or(() -> find(ANY_DATE).flatMap(this::date));
  }

  /**
   * The labelled due date, otherwise one from a payment term in days.
   */
  Optional<LocalDate> dueDate(LocalDate invoiceDate) {
    return find(vocabulary.dueDate()).flatMap(this::date)
      .or(() -> find(vocabulary.paymentTerm()).map(Long::parseLong)
        .map(invoiceDate::plusDays));
  }

  Optional<String> iban() {
    return IbanSearch.of(text).first();
  }

  String currency() {
    var upper = text.toUpperCase(Locale.ROOT);
    if (upper.contains("€") || upper.contains("EUR")) {
      return "EUR";
    }
    return Stream.of("CHF", "USD", "GBP").filter(upper::contains)
      .findFirst().orElse(upper.contains("$") ? "USD" : "EUR");
  }

  private Optional<LocalDate> date(String value) {
    return PrintedDate.of(value, vocabulary.months()).date();
  }

  private Optional<String> find(Pattern pattern) {
    return pattern.matcher(text).results().findFirst()
      .flatMap(this::value);
  }

  /**
   * The first group that matched; phrases with a placeholder each bring
   * their own group.
   */
  private Optional<String> value(MatchResult result) {
    return IntStream.rangeClosed(1, result.groupCount())
      .mapToObj(result::group)
      .filter(Objects::nonNull)
      .findFirst()
      .map(String::trim);
  }
}