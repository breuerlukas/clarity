package de.lukasbreuer.clarity.api.business.invoice.matching;

import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Locale;

/**
 * Finds an invoice number in a transfer's reference, where payers write it
 * in any form: "RE-2026-042" may arrive as "RE 2026 042", "re2026042" or
 * "Rechnung RE-2026-042". Long numbers are compared without separators;
 * short ones must be a word of their own (or follow letters, as in
 * "RE1042"), so "104" is not found inside an IBAN or an amount.
 */
@RequiredArgsConstructor(staticName = "of")
public final class InvoiceNumber {
  private static final int MINIMUM_LENGTH = 3;
  private static final int SQUASHED_MINIMUM_LENGTH = 6;

  private final String number;

  public boolean appearsIn(String text) {
    if (squashed().length() < MINIMUM_LENGTH || text == null) {
      return false;
    }
    return appearsSquashedIn(text) || appearsAsWordIn(text);
  }

  /**
   * The number in upper case without separators.
   */
  public String squashed() {
    return number.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
  }

  /**
   * Whether both numbers are the same apart from case and separators.
   */
  public boolean sameAs(String otherNumber) {
    return squashed().equals(InvoiceNumber.of(otherNumber).squashed());
  }

  private boolean appearsSquashedIn(String text) {
    return squashed().length() >= SQUASHED_MINIMUM_LENGTH &&
      InvoiceNumber.of(text).squashed().contains(squashed());
  }

  private boolean appearsAsWordIn(String text) {
    return Arrays.stream(text.toUpperCase(Locale.ROOT).split("[^A-Z0-9]+"))
      .anyMatch(this::isWritten);
  }

  /**
   * @param word A word of the reference, in upper case
   */
  private boolean isWritten(String word) {
    var squashed = squashed();
    return word.equals(squashed) || (word.endsWith(squashed) &&
      Character.isLetter(word.charAt(word.length() - squashed.length() - 1)));
  }
}