package de.lukasbreuer.clarity.api.business.invoice.reading;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class IbanSearchTest {
  @Test
  void findsIbanWrittenWithSpaces() {
    assertEquals("DE89370400440532013000",
      IbanSearch.of("IBAN DE89 3704 0044 0532 0130 00 BIC COBADEFFXXX")
        .first().orElseThrow());
  }

  @Test
  void findsOnlyValidIbans() {
    assertTrue(IbanSearch.of("Kundennummer DE12 3456 7890 1234 5678 90")
      .first().isEmpty());
  }

  @Test
  void normalizesAndValidates() {
    var iban = Iban.of(" de89 3704 0044 0532 0130 00");
    assertEquals("DE89370400440532013000", iban.normalized());
    assertTrue(iban.isValid());
    assertFalse(Iban.of("DE12345678901234567890").isValid());
  }
}