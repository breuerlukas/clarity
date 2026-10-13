package de.lukasbreuer.clarity.api.business.invoice.reading;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TextInvoiceReaderTest {
  private static final LocalDate RECEIVED = LocalDate.of(2026, 10, 5);

  @Test
  void readsGermanInvoice() {
    var text = """
      Druckerei Weber e.K.
      Rechnung
      Rechnungsnummer: 2026-1187
      Rechnungsdatum: 01.10.2026
      Pos. Menge Beschreibung Betrag
      1 500 Flyer A5 250,00 €
      Nettobetrag 250,00 €
      Umsatzsteuer 19 % 47,50 €
      Rechnungsbetrag 297,50 €
      Zahlbar innerhalb von 14 Tagen ohne Abzug.
      IBAN: DE89 3704 0044 0532 0130 00
      """;
    var fields = TextInvoiceReader.read(text, RECEIVED).orElseThrow();
    assertEquals("2026-1187", fields.number());
    assertEquals(LocalDate.of(2026, 10, 1), fields.invoiceDate());
    assertEquals(LocalDate.of(2026, 10, 15), fields.dueDate());
    assertEquals(new BigDecimal("250.00"), fields.netAmount());
    assertEquals(new BigDecimal("47.50"), fields.taxAmount());
    assertEquals(new BigDecimal("297.50"), fields.grossAmount());
    assertEquals("DE89370400440532013000", fields.iban());
    assertEquals("EUR", fields.currency());
  }

  @Test
  void readsEnglishInvoice() {
    var text = """
      INVOICE
      Invoice No. INV-7731
      Date: 2026-09-30
      Subtotal 1,000.00 USD
      VAT 0.00
      Total due: 1,000.00 USD
      Due date: 2026-10-30
      """;
    var fields = TextInvoiceReader.read(text, RECEIVED).orElseThrow();
    assertEquals("INV-7731", fields.number());
    assertEquals(LocalDate.of(2026, 9, 30), fields.invoiceDate());
    assertEquals(LocalDate.of(2026, 10, 30), fields.dueDate());
    assertEquals(new BigDecimal("1000.00"), fields.grossAmount());
    assertEquals("USD", fields.currency());
    assertNull(fields.iban());
  }

  @Test
  void ignoresOtherDocuments() {
    assertTrue(TextInvoiceReader.read("""
      Angebot
      Die Rechnung erhalten Sie nach Lieferung.
      Gesamtbetrag 1.190,00 €
      """, RECEIVED).isEmpty());
    assertTrue(TextInvoiceReader.read("""
      Rechnung
      Vielen Dank für Ihre Bestellung.
      """, RECEIVED).isEmpty());
  }

  @Test
  void parsesAmounts() {
    assertEquals(new BigDecimal("1234.56"), Amounts.parse("1.234,56 €")
      .orElseThrow());
    assertEquals(new BigDecimal("1234.56"), Amounts.parse("1,234.56")
      .orElseThrow());
    assertEquals(new BigDecimal("1234.00"), Amounts.parse("1.234")
      .orElseThrow());
    assertEquals(new BigDecimal("12.50"), Amounts.parse("12,5")
      .orElseThrow());
    assertTrue(Amounts.parse("EUR").isEmpty());
  }

  @Test
  void findsOnlyValidIbans() {
    assertEquals("DE89370400440532013000",
      Iban.find("IBAN DE89 3704 0044 0532 0130 00 BIC COBADEFFXXX")
        .orElseThrow());
    assertTrue(Iban.find("Kundennummer DE12 3456 7890 1234 5678 90")
      .isEmpty());
  }
}
