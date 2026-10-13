package de.lukasbreuer.clarity.api.business.invoice.reading;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Finds IBANs in text, written with or without the usual spaces, and keeps
 * only those whose check digits are right, so a reference number that
 * happens to look like one is not taken.
 */
public final class Iban {
  private static final Pattern CANDIDATE = Pattern.compile(
    "\\b([A-Z]{2}[0-9]{2}(?: ?[A-Z0-9]{4}){2,7}(?: ?[A-Z0-9]{1,4})?)\\b");
  private static final BigInteger NINETY_SEVEN = BigInteger.valueOf(97);

  private Iban() {
  }

  public static Optional<String> find(String text) {
    var matcher = CANDIDATE.matcher(text.toUpperCase());
    while (matcher.find()) {
      var iban = longestValid(matcher.group(1).split(" "));
      if (iban.isPresent()) {
        return iban;
      }
    }
    return Optional.empty();
  }

  /**
   * The match may have taken a group of the text after the IBAN, so groups
   * are dropped from the end until the check digits fit.
   */
  private static Optional<String> longestValid(String[] groups) {
    for (var count = groups.length; count > 0; count--) {
      var iban = String.join("", Arrays.copyOf(groups, count));
      if (isValid(iban)) {
        return Optional.of(iban);
      }
    }
    return Optional.empty();
  }

  public static String normalize(String iban) {
    return iban.replaceAll("\\s", "").toUpperCase();
  }

  public static boolean isValid(String iban) {
    if (iban.length() < 15 || iban.length() > 34 ||
      !iban.matches("[A-Z]{2}[0-9]{2}[A-Z0-9]+")) {
      return false;
    }
    var rearranged = iban.substring(4) + iban.substring(0, 4);
    var digits = new StringBuilder();
    for (var character : rearranged.toCharArray()) {
      digits.append(Character.getNumericValue(character));
    }
    return new BigInteger(digits.toString()).mod(NINETY_SEVEN)
      .equals(BigInteger.ONE);
  }
}
