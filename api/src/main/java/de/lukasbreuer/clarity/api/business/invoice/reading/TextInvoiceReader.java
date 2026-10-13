package de.lukasbreuer.clarity.api.business.invoice.reading;

import de.lukasbreuer.clarity.api.business.invoice.InvoiceFields;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Guesses the fields of an invoice from its text, in German or English. Text
 * only counts as an invoice when it names a total and either an invoice
 * number or a heading that says invoice (and no other heading such as an
 * offer or a credit note); everything read here is a guess the owner
 * confirms. The parties are left empty, the
 * import takes them from the mail.
 */
public final class TextInvoiceReader {
  private static final int FLAGS = Pattern.CASE_INSENSITIVE |
    Pattern.UNICODE_CASE;
  private static final String AMOUNT =
    "(-?(?:\\d{1,3}(?:[.,' ]\\d{3})+|\\d+)(?:[.,]\\d{1,2})?)";
  private static final String GAP = "[^\\n\\d]{0,40}?";
  private static final String DATE =
    "(\\d{1,2}\\.\\s?\\d{1,2}\\.\\s?\\d{2,4}|\\d{4}-\\d{2}-\\d{2}|" +
      "\\d{1,2}/\\d{1,2}/\\d{4}|\\d{1,2}\\.?\\s+\\p{L}+\\s+\\d{4})";
  private static final Pattern HEADING = Pattern.compile(
    "^\\s*(rechnung|invoice|faktura)\\b", FLAGS | Pattern.MULTILINE);
  private static final Pattern OTHER_DOCUMENT = Pattern.compile(
    "^\\s*(angebot|kostenvoranschlag|lieferschein|auftragsbestätigung|" +
      "gutschrift|quote|quotation|offer|delivery note|credit note)\\b",
    FLAGS | Pattern.MULTILINE);
  private static final Pattern NUMBER = Pattern.compile(
    "(?:rechnungs-?\\s?(?:nummer|nr\\.?)|rechnung\\s*(?:nr\\.?|#)|" +
      "invoice\\s*(?:no\\.?|number|#)|beleg-?\\s?(?:nummer|nr\\.?))" +
      "\\s*[:.]?\\s*([A-Z0-9][A-Z0-9\\-/_.]{1,30}[A-Z0-9])", FLAGS);
  private static final List<Pattern> GROSS = labels(
    "rechnungsbetrag", "gesamtbetrag", "bruttobetrag", "endbetrag",
    "zahlbetrag", "zu zahlen(?:der betrag)?", "gesamt brutto",
    "summe brutto", "brutto ?summe", "amount due", "total due",
    "total amount", "gesamtsumme", "total", "summe", "gesamt");
  private static final List<Pattern> NET = labels(
    "nettobetrag", "summe netto", "netto ?summe", "gesamt netto",
    "zwischensumme", "subtotal", "net amount", "netto");
  private static final Pattern TAX = Pattern.compile(
    "(?:umsatzsteuer|ust\\.?|mwst\\.?|mehrwertsteuer|vat|sales tax)" +
      "(?:[^\\n\\d]{0,20}\\d{1,2}(?:[.,]\\d{1,2})?\\s?%)?" + GAP + AMOUNT,
    FLAGS);
  private static final Pattern INVOICE_DATE = Pattern.compile(
    "(?:rechnungsdatum|belegdatum|invoice date|datum|date)\\s*[:.]?\\s*" +
      DATE, FLAGS);
  private static final Pattern ANY_DATE = Pattern.compile(DATE, FLAGS);
  private static final Pattern DUE_DATE = Pattern.compile(
    "(?:fällig(?:keit)?(?:\\s*(?:am|bis|zum))?|zahlbar bis|zahlungsziel|" +
      "due date|due|payable by)\\s*[:.]?\\s*" + DATE, FLAGS);
  private static final Pattern DUE_DAYS = Pattern.compile(
    "(?:innerhalb|binnen|within)\\s+(?:von\\s+)?(\\d{1,3})\\s+(?:tagen|days)",
    FLAGS);
  private static final Map<String, Integer> MONTHS = Map.ofEntries(
    Map.entry("januar", 1), Map.entry("january", 1),
    Map.entry("februar", 2), Map.entry("february", 2),
    Map.entry("märz", 3), Map.entry("march", 3), Map.entry("april", 4),
    Map.entry("mai", 5), Map.entry("may", 5), Map.entry("juni", 6),
    Map.entry("june", 6), Map.entry("juli", 7), Map.entry("july", 7),
    Map.entry("august", 8), Map.entry("september", 9),
    Map.entry("oktober", 10), Map.entry("october", 10),
    Map.entry("november", 11), Map.entry("dezember", 12),
    Map.entry("december", 12));

  private TextInvoiceReader() {
  }

  private static List<Pattern> labels(String... labels) {
    return Arrays.stream(labels)
      .map(label -> Pattern.compile("\\b" + label + "\\b" + GAP + AMOUNT,
        FLAGS))
      .toList();
  }

  /**
   * @param received When the mail arrived, the invoice date if the text has
   *                 none
   */
  public static Optional<InvoiceFields> read(String text, LocalDate received) {
    var number = find(NUMBER, text);
    if (number.isEmpty() && (!HEADING.matcher(text).find() ||
      OTHER_DOCUMENT.matcher(text).find())) {
      return Optional.empty();
    }
    var gross = firstLabelled(text, GROSS);
    if (gross.isEmpty() || gross.get().signum() <= 0) {
      return Optional.empty();
    }
    var invoiceDate = find(INVOICE_DATE, text).flatMap(TextInvoiceReader::date)
      .or(() -> find(ANY_DATE, text).flatMap(TextInvoiceReader::date))
      .orElse(received);
    var dueDate = find(DUE_DATE, text).flatMap(TextInvoiceReader::date)
      .or(() -> find(DUE_DAYS, text).map(Long::parseLong)
        .map(invoiceDate::plusDays));
    return Optional.of(new InvoiceFields(
      number.orElse(""), "", "",
      Iban.find(text).orElse(null), currency(text),
      firstLabelled(text, NET).filter(net -> net.compareTo(gross.get()) <= 0)
        .orElse(null),
      find(TAX, text).flatMap(Amounts::parse)
        .filter(tax -> tax.compareTo(gross.get()) < 0).orElse(null),
      gross.get(), invoiceDate, dueDate.orElse(null)));
  }

  /**
   * The amount after the first label that appears, in the order of the list;
   * for a label printed several times the largest amount, since subtotals
   * come before the total.
   */
  private static Optional<BigDecimal> firstLabelled(
    String text, List<Pattern> labels
  ) {
    for (var label : labels) {
      var amounts = label.matcher(text).results()
        .map(result -> result.group(1))
        .map(Amounts::parse)
        .flatMap(Optional::stream)
        .toList();
      if (!amounts.isEmpty()) {
        return amounts.stream().max(Comparator.naturalOrder());
      }
    }
    return Optional.empty();
  }

  private static Optional<String> find(Pattern pattern, String text) {
    Matcher matcher = pattern.matcher(text);
    return matcher.find() ? Optional.of(matcher.group(1).trim()) :
      Optional.empty();
  }

  private static String currency(String text) {
    var upper = text.toUpperCase(Locale.ROOT);
    if (upper.contains("€") || upper.contains("EUR")) {
      return "EUR";
    }
    for (var currency : List.of("CHF", "USD", "GBP")) {
      if (upper.contains(currency)) {
        return currency;
      }
    }
    return upper.contains("$") ? "USD" : "EUR";
  }

  static Optional<LocalDate> date(String value) {
    try {
      var compact = value.replaceAll("\\s", "");
      if (compact.matches("\\d{4}-\\d{2}-\\d{2}")) {
        return Optional.of(LocalDate.parse(compact));
      }
      if (compact.matches("\\d{1,2}\\.\\d{1,2}\\.\\d{2,4}")) {
        var parts = compact.split("\\.");
        var year = Integer.parseInt(parts[2]);
        return Optional.of(LocalDate.of(year < 100 ? 2000 + year : year,
          Integer.parseInt(parts[1]), Integer.parseInt(parts[0])));
      }
      if (compact.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
        var parts = compact.split("/");
        return Optional.of(LocalDate.of(Integer.parseInt(parts[2]),
          Integer.parseInt(parts[1]), Integer.parseInt(parts[0])));
      }
      var words = value.trim().split("\\s+");
      var month = MONTHS.get(words[1].toLowerCase(Locale.GERMAN));
      if (words.length == 3 && month != null) {
        return Optional.of(LocalDate.of(Integer.parseInt(words[2]), month,
          Integer.parseInt(words[0].replace(".", ""))));
      }
    } catch (RuntimeException exception) {
      return Optional.empty();
    }
    return Optional.empty();
  }
}
