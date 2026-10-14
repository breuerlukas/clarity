package de.lukasbreuer.clarity.api.business.invoice.reading;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TextInvoiceReaderTest {
  private static final LocalDate RECEIVED = LocalDate.of(2026, 10, 5);
  private static final String GERMAN_INVOICE = """
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
  private static final String ENGLISH_INVOICE = """
    INVOICE
    Invoice No. INV-7731
    Date: 2026-09-30
    Subtotal 1,000.00 USD
    VAT 0.00
    Total due: 1,000.00 USD
    Due date: 2026-10-30
    """;

  private final TextInvoiceReader reader = TextInvoiceReader.create(
    InvoiceVocabularyFile.load().vocabulary());

  @Test
  void readsGermanInvoice() {
    var fields = reader.read(GERMAN_INVOICE, RECEIVED).orElseThrow();
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
    var fields = reader.read(ENGLISH_INVOICE, RECEIVED).orElseThrow();
    assertEquals("INV-7731", fields.number());
    assertEquals(LocalDate.of(2026, 9, 30), fields.invoiceDate());
    assertEquals(LocalDate.of(2026, 10, 30), fields.dueDate());
    assertEquals(new BigDecimal("1000.00"), fields.grossAmount());
    assertEquals("USD", fields.currency());
    assertNull(fields.iban());
  }

  @Test
  void ignoresOtherDocuments() {
    assertTrue(reader.read("""
      Angebot
      Die Rechnung erhalten Sie nach Lieferung.
      Gesamtbetrag 1.190,00 €
      """, RECEIVED).isEmpty());
    assertTrue(reader.read("""
      Rechnung
      Vielen Dank für Ihre Bestellung.
      """, RECEIVED).isEmpty());
  }

  @Test
  void readsHyphenatedLabelsAndPaymentTerms() {
    var fields = reader.read("""
      Rechnungs-Nr.: 2026-77
      Datum: 3. Oktober 2026
      Zu zahlender Betrag: 59,50 €
      Zahlbar innerhalb von 14 Tagen.
      """, RECEIVED).orElseThrow();
    assertEquals("2026-77", fields.number());
    assertEquals(LocalDate.of(2026, 10, 3), fields.invoiceDate());
    assertEquals(LocalDate.of(2026, 10, 17), fields.dueDate());
    assertEquals(new BigDecimal("59.50"), fields.grossAmount());
  }
}