package de.lukasbreuer.clarity.api.business.invoice.matching;

import java.util.Arrays;
import java.util.Locale;

/**
 * Finds an invoice number in a transfer's reference, where payers write it
 * in any form: "RE-2026-042" may arrive as "RE 2026 042", "re2026042" or
 * "Rechnung RE-2026-042". Long numbers are compared without separators;
 * short ones must be a word of their own (or follow letters, as in
 * "RE1042"), so "104" is not found inside an IBAN or an amount.
 */
public final class InvoiceNumber {
  private static final int MINIMUM_LENGTH = 3;
  private static final int SQUASHED_MINIMUM_LENGTH = 6;

  private InvoiceNumber() {
  }

  public static boolean appearsIn(String number, String text) {
    var squashed = squash(number);
    if (squashed.length() < MINIMUM_LENGTH || text == null) {
      return false;
    }
    if (squashed.length() >= SQUASHED_MINIMUM_LENGTH &&
      squash(text).contains(squashed)) {
      return true;
    }
    return Arrays.stream(text.toUpperCase(Locale.ROOT).split("[^A-Z0-9]+"))
      .anyMatch(word -> word.equals(squashed) || (word.endsWith(squashed) &&
        Character.isLetter(word.charAt(word.length() - squashed.length() -
          1))));
  }

  public static String squash(String value) {
    return value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
  }
}
