package de.lukasbreuer.clarity.api.business.invoice.reading;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ElectronicInvoiceReaderTest {
  @Test
  void readsCrossIndustryInvoice() throws IOException {
    var fields = ElectronicInvoiceReader.read(file("xrechnung-cii.xml"))
      .orElseThrow();
    assertEquals("RE-2026-0042", fields.number());
    assertEquals("Nordlicht Hosting GmbH", fields.issuer());
    assertEquals("Breuer Software UG", fields.recipient());
    assertEquals("DE89370400440532013000", fields.iban());
    assertEquals("EUR", fields.currency());
    assertEquals(new BigDecimal("100.00"), fields.netAmount());
    assertEquals(new BigDecimal("19.00"), fields.taxAmount());
    assertEquals(new BigDecimal("119.00"), fields.grossAmount());
    assertEquals(LocalDate.of(2026, 9, 15), fields.invoiceDate());
    assertEquals(LocalDate.of(2026, 9, 29), fields.dueDate());
  }

  @Test
  void readsUblInvoice() throws IOException {
    var fields = ElectronicInvoiceReader.read(file("xrechnung-ubl.xml"))
      .orElseThrow();
    assertEquals("2026/118", fields.number());
    assertEquals("Breuer Software UG", fields.issuer());
    assertEquals("Müller Logistik GmbH", fields.recipient());
    assertEquals("DE02120300000000202051", fields.iban());
    assertEquals(new BigDecimal("1200.00"), fields.netAmount());
    assertEquals(new BigDecimal("228.00"), fields.taxAmount());
    assertEquals(new BigDecimal("1428.00"), fields.grossAmount());
    assertEquals(LocalDate.of(2026, 10, 1), fields.invoiceDate());
    assertEquals(LocalDate.of(2026, 10, 15), fields.dueDate());
  }

  @Test
  void refusesExternalEntities() throws IOException {
    assertTrue(ElectronicInvoiceReader.read(file("xxe.xml")).isEmpty());
  }

  @Test
  void refusesEntityExpansion() throws IOException {
    assertTrue(ElectronicInvoiceReader.read(file("entity-expansion.xml"))
      .isEmpty());
  }

  @Test
  void ignoresOtherXml() {
    assertTrue(ElectronicInvoiceReader.read("<order><id>1</id></order>"
      .getBytes()).isEmpty());
    assertTrue(ElectronicInvoiceReader.read("not xml".getBytes()).isEmpty());
  }

  static byte[] file(String name) throws IOException {
    try (var stream = ElectronicInvoiceReaderTest.class.getResourceAsStream(
      "/invoices/" + name)) {
      return stream.readAllBytes();
    }
  }
}
