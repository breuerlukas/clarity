package de.lukasbreuer.clarity.api.business.invoice.reading;

import lombok.RequiredArgsConstructor;

import java.math.BigInteger;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * An IBAN as written somewhere, with or without the usual spaces. It is
 * only valid when its check digits are right, so a reference number that
 * happens to look like one is not taken.
 */
@RequiredArgsConstructor(staticName = "of")
public final class Iban {
  private static final int MINIMUM_LENGTH = 15;
  private static final int MAXIMUM_LENGTH = 34;
  private static final BigInteger NINETY_SEVEN = BigInteger.valueOf(97);

  private final String text;

  public String normalized() {
    return text.replaceAll("\\s", "").toUpperCase(Locale.ROOT);
  }

  public boolean isValid() {
    var iban = normalized();
    return hasIbanShape(iban) && checksum(iban).equals(BigInteger.ONE);
  }

  private boolean hasIbanShape(String iban) {
    return iban.length() >= MINIMUM_LENGTH &&
      iban.length() <= MAXIMUM_LENGTH &&
      iban.matches("[A-Z]{2}[0-9]{2}[A-Z0-9]+");
  }

  /**
   * ISO 13616: the country and check digits move to the end, letters become
   * numbers (A = 10), and the whole number modulo 97 is 1.
   */
  private BigInteger checksum(String iban) {
    var rearranged = iban.substring(4) + iban.substring(0, 4);
    var digits = rearranged.chars()
      .mapToObj(character -> String.valueOf(Character.getNumericValue(
        character)))
      .collect(Collectors.joining());
    return new BigInteger(digits).mod(NINETY_SEVEN);
  }
}