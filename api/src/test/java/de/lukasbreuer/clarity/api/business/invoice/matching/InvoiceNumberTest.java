package de.lukasbreuer.clarity.api.business.invoice.matching;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class InvoiceNumberTest {
  @Test
  void findsNumbersInAnyForm() {
    assertTrue(InvoiceNumber.of("RE-2026-042").appearsIn("re2026042"));
    assertTrue(InvoiceNumber.of("1042").appearsIn("Rechnung 1042 danke"));
    assertTrue(InvoiceNumber.of("1042").appearsIn("RE1042"));
    assertFalse(InvoiceNumber.of("104").appearsIn("DE89104200 Betrag 1042"));
    assertFalse(InvoiceNumber.of("12").appearsIn("12"));
  }

  @Test
  void comparesNumbersWithoutSeparators() {
    assertTrue(InvoiceNumber.of("RE-2026-042").sameAs("re 2026/042"));
    assertFalse(InvoiceNumber.of("RE-2026-042").sameAs("RE-2026-043"));
  }
}