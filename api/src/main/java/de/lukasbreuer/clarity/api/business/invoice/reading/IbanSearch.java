package de.lukasbreuer.clarity.api.business.invoice.reading;

import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * Finds the first valid IBAN in a text, written with or without the usual
 * spaces.
 */
@RequiredArgsConstructor(staticName = "of")
public final class IbanSearch {
  private static final Pattern CANDIDATE = Pattern.compile(
    "\\b([A-Z]{2}[0-9]{2}(?: ?[A-Z0-9]{4}){2,7}(?: ?[A-Z0-9]{1,4})?)\\b");

  private final String text;

  /**
   * @return The IBAN without spaces
   */
  public Optional<String> first() {
    return CANDIDATE.matcher(text.toUpperCase(Locale.ROOT)).results()
      .map(result -> longestValid(result.group(1).split(" ")))
      .flatMap(Optional::stream)
      .findFirst();
  }

  /**
   * The match may have taken a group of the text after the IBAN, so groups
   * are dropped from the end until the check digits fit.
   */
  private Optional<String> longestValid(String[] groups) {
    return IntStream.iterate(groups.length, count -> count > 0,
        count -> count - 1)
      .mapToObj(count -> String.join("", Arrays.copyOf(groups, count)))
      .filter(candidate -> Iban.of(candidate).isValid())
      .findFirst();
  }
}