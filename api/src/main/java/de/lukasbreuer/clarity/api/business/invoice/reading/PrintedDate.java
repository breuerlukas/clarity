package de.lukasbreuer.clarity.api.business.invoice.reading;

import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * A date as invoices print it: "01.10.2026", "1.10.26", "2026-10-01",
 * "01/10/2026" (day first) or "1. Oktober 2026" / "1 October 2026", with
 * the month names of the invoice vocabulary.
 */
@RequiredArgsConstructor(staticName = "of")
final class PrintedDate {
  /**
   * Finds such a date in a text, as group 1.
   */
  static final String PATTERN =
    "(\\d{1,2}\\.\\s?\\d{1,2}\\.\\s?\\d{2,4}|\\d{4}-\\d{2}-\\d{2}|" +
      "\\d{1,2}/\\d{1,2}/\\d{4}|\\d{1,2}\\.?\\s+\\p{L}+\\s+\\d{4})";
  private final String value;
  private final Map<String, Integer> months;

  Optional<LocalDate> date() {
    try {
      return parse(value.replaceAll("\\s", ""));
    } catch (RuntimeException exception) {
      return Optional.empty();
    }
  }

  private Optional<LocalDate> parse(String compact) {
    if (compact.matches("\\d{4}-\\d{2}-\\d{2}")) {
      return Optional.of(LocalDate.parse(compact));
    }
    if (compact.matches("\\d{1,2}\\.\\d{1,2}\\.\\d{2,4}")) {
      return Optional.of(dayMonthYear(compact.split("\\.")));
    }
    if (compact.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
      return Optional.of(dayMonthYear(compact.split("/")));
    }
    return writtenOut(value.trim().split("\\s+"));
  }

  /**
   * Two-digit years are taken for this century.
   */
  private LocalDate dayMonthYear(String[] parts) {
    var year = Integer.parseInt(parts[2]);
    return LocalDate.of(year < 100 ? 2000 + year : year,
      Integer.parseInt(parts[1]), Integer.parseInt(parts[0]));
  }

  private Optional<LocalDate> writtenOut(String[] words) {
    var month = Optional.ofNullable(months.get(words[1]
      .toLowerCase(Locale.GERMAN)));
    return month.filter(_ -> words.length == 3)
      .map(number -> LocalDate.of(Integer.parseInt(words[2]), number,
        Integer.parseInt(words[0].replace(".", ""))));
  }
}