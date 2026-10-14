package de.lukasbreuer.clarity.api.business.invoice.reading;

import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The text of a PDF or scan that may be an invoice, in German or English,
 * and the invoice fields that can be guessed from it.
 */
@RequiredArgsConstructor(staticName = "of")
final class InvoiceText {
  private static final int FLAGS = AmountLabels.FLAGS;
  private static final Pattern HEADING = Pattern.compile(
    "^\\s*(rechnung|invoice|faktura)\\b",
    FLAGS | Pattern.MULTILINE);
  private static final Pattern OTHER_DOCUMENT = Pattern.compile(
    "^\\s*(angebot|kostenvoranschlag|lieferschein|auftragsbestätigung|" +
      "gutschrift|quote|quotation|offer|delivery note|credit note)\\b",
    FLAGS | Pattern.MULTILINE);
  private static final Pattern NUMBER = Pattern.compile(
    "(?:rechnungs-?\\s?(?:nummer|nr\\.?)|rechnung\\s*(?:nr\\.?|#)|" +
      "invoice\\s*(?:no\\.?|number|#)|beleg-?\\s?(?:nummer|nr\\.?))" +
      "\\s*[:.]?\\s*([A-Z0-9][A-Z0-9\\-/_.]{1,30}[A-Z0-9])",
    FLAGS);
  private static final AmountLabels GROSS = AmountLabels.of(
    "rechnungsbetrag", "gesamtbetrag", "bruttobetrag", "endbetrag",
    "zahlbetrag", "zu zahlen(?:der betrag)?", "gesamt brutto",
    "summe brutto", "brutto ?summe", "amount due", "total due",
    "total amount", "gesamtsumme", "total", "summe", "gesamt");
  private static final AmountLabels NET = AmountLabels.of(
    "nettobetrag", "summe netto", "netto ?summe", "gesamt netto",
    "zwischensumme", "subtotal", "net amount", "netto");
  private static final Pattern TAX = Pattern.compile(
    "(?:umsatzsteuer|ust\\.?|mwst\\.?|mehrwertsteuer|vat|sales tax)" +
      "(?:[^\\n\\d]{0,20}\\d{1,2}(?:[.,]\\d{1,2})?\\s?%)?" + AmountLabels.GAP +
      AmountLabels.AMOUNT, FLAGS);
  private static final Pattern INVOICE_DATE = Pattern.compile(
    "(?:rechnungsdatum|belegdatum|invoice date|datum|date)\\s*[:.]?\\s*" +
      PrintedDate.PATTERN, FLAGS);
  private static final Pattern ANY_DATE =
    Pattern.compile(PrintedDate.PATTERN, FLAGS);
  private static final Pattern DUE_DATE = Pattern.compile(
    "(?:fällig(?:keit)?(?:\\s*(?:am|bis|zum))?|zahlbar bis|zahlungsziel|" +
      "due date|due|payable by)\\s*[:.]?\\s*" + PrintedDate.PATTERN,
    FLAGS);
  private static final Pattern DUE_DAYS = Pattern.compile(
    "(?:innerhalb|binnen|within)\\s+(?:von\\s+)?(\\d{1,3})\\s+(?:tagen|days)",
    FLAGS);

  private final String text;

  /**
   * It names an invoice number, or has a heading that says invoice and
   * none that says it is something else, such as an offer or a credit note.
   */
  boolean isInvoice() {
    return number().isPresent() || (HEADING.matcher(text).find() &&
      !OTHER_DOCUMENT.matcher(text).find());
  }

  Optional<String> number() {
    return find(NUMBER);
  }

  Optional<BigDecimal> gross() {
    return GROSS.firstIn(text);
  }

  Optional<BigDecimal> net(BigDecimal gross) {
    return NET.firstIn(text).filter(net -> net.compareTo(gross) <= 0);
  }

  Optional<BigDecimal> tax(BigDecimal gross) {
    return find(TAX).flatMap(tax -> AmountText.of(tax).value())
      .filter(tax -> tax.compareTo(gross) < 0);
  }

  /**
   * The labelled invoice date, otherwise the first date in the text.
   */
  Optional<LocalDate> invoiceDate() {
    return find(INVOICE_DATE).flatMap(this::date)
      .or(() -> find(ANY_DATE).flatMap(this::date));
  }

  /**
   * The labelled due date, otherwise one from a payment term in days.
   */
  Optional<LocalDate> dueDate(LocalDate invoiceDate) {
    return find(DUE_DATE).flatMap(this::date)
      .or(() -> find(DUE_DAYS).map(Long::parseLong)
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
    return PrintedDate.of(value).date();
  }

  private Optional<String> find(Pattern pattern) {
    return pattern.matcher(text).results().findFirst()
      .map(result -> result.group(1).trim());
  }
}