package de.lukasbreuer.clarity.api.business.invoice.reading;

import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Labels as {@code invoice-vocabulary.json} and
 * {@code receipt-vocabulary.json} write them: plain phrases in
 * which a space also stands for a hyphen or no space at all, so
 * "rechnungs nr" finds "Rechnungsnr", "Rechnungs-Nr" and "Rechnungs Nr",
 * and {@code {days}} stands for a number of days.
 */
@RequiredArgsConstructor(staticName = "of")
public final class LabelPhrases {
  private static final String SEPARATOR = "[\\s-]*";
  private static final String DAYS = "{days}";
  private static final String DAYS_EXPRESSION = "(\\d{1,3})";

  private final List<String> phrases;

  /**
   * One expression for any of the phrases, longer ones first, so that
   * "fällig am" wins over "fällig".
   */
  public String anyOf() {
    return phrases.stream()
      .sorted(Comparator.comparingInt(String::length).reversed())
      .map(this::expression)
      .collect(Collectors.joining("|", "(?:", ")"));
  }

  /**
   * One expression per phrase, in the order of the file.
   */
  List<String> each() {
    return phrases.stream().map(this::expression).toList();
  }

  private String expression(String phrase) {
    return Arrays.stream(phrase.trim().split("\\s+"))
      .map(this::word)
      .collect(Collectors.joining(SEPARATOR));
  }

  private String word(String word) {
    return word.equals(DAYS) ? DAYS_EXPRESSION : Pattern.quote(word);
  }
}