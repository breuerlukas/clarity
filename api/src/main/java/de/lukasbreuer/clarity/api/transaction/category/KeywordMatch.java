package de.lukasbreuer.clarity.api.transaction.category;

import lombok.RequiredArgsConstructor;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Whether a keyword appears in a text at the start of a word, ignoring case.
 * "rewe" matches "REWE Markt GmbH" but "tier" does not match "Kuriertier"
 * or "Rentier".
 */
@RequiredArgsConstructor(staticName = "of")
public final class KeywordMatch {
  private final String text;
  private final String keyword;

  public boolean matches() {
    var normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
    return Pattern.compile("(^|[^\\p{L}\\p{N}])" +
        Pattern.quote(normalizedKeyword))
      .matcher(text.toLowerCase(Locale.ROOT))
      .find();
  }
}