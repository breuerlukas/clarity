package de.lukasbreuer.clarity.api.business.invoice.reading;

import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * A date as invoices print it: "01.10.2026", "1.10.26", "2026-10-01",
 * "01/10/2026" (day first) or "1. Oktober 2026" / "1 October 2026".
 */
@RequiredArgsConstructor(staticName = "of")
final class PrintedDate {
  /**
   * Finds such a date in a text, as group 1.
   */
  static final String PATTERN =
    "(\\d{1,2}\\.\\s?\\d{1,2}\\.\\s?\\d{2,4}|\\d{4}-\\d{2}-\\d{2}|" +
      "\\d{1,2}/\\d{1,2}/\\d{4}|\\d{1,2}\\.?\\s+\\p{L}+\\s+\\d{4})";
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

  private final String value;

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
    var month = Optional.ofNullable(MONTHS.get(words[1]
      .toLowerCase(Locale.GERMAN)));
    return month.filter(_ -> words.length == 3)
      .map(number -> LocalDate.of(Integer.parseInt(words[2]), number,
        Integer.parseInt(words[0].replace(".", ""))));
  }
}