package de.lukasbreuer.clarity.api.business.invoice.reading;

import de.lukasbreuer.clarity.api.business.invoice.InvoiceFields;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ElectronicInvoiceReaderTest {
  private final ElectronicInvoiceReader reader =
    ElectronicInvoiceReader.create(SecureXml.create());

  @Test
  void readsCrossIndustryInvoiceParties() throws IOException {
    var fields = read("xrechnung-cii.xml");
    assertEquals("RE-2026-0042", fields.number());
    assertEquals("Nordlicht Hosting GmbH", fields.issuer());
    assertEquals("Breuer Software UG", fields.recipient());
    assertEquals("DE89370400440532013000", fields.iban());
    assertEquals(LocalDate.of(2026, 9, 15), fields.invoiceDate());
    assertEquals(LocalDate.of(2026, 9, 29), fields.dueDate());
  }

  @Test
  void readsCrossIndustryInvoiceAmounts() throws IOException {
    var fields = read("xrechnung-cii.xml");
    assertEquals("EUR", fields.currency());
    assertEquals(new BigDecimal("100.00"), fields.netAmount());
    assertEquals(new BigDecimal("19.00"), fields.taxAmount());
    assertEquals(new BigDecimal("119.00"), fields.grossAmount());
  }

  @Test
  void readsUblInvoiceParties() throws IOException {
    var fields = read("xrechnung-ubl.xml");
    assertEquals("2026/118", fields.number());
    assertEquals("Breuer Software UG", fields.issuer());
    assertEquals("Müller Logistik GmbH", fields.recipient());
    assertEquals("DE02120300000000202051", fields.iban());
    assertEquals(LocalDate.of(2026, 10, 1), fields.invoiceDate());
    assertEquals(LocalDate.of(2026, 10, 15), fields.dueDate());
  }

  @Test
  void readsUblInvoiceAmounts() throws IOException {
    var fields = read("xrechnung-ubl.xml");
    assertEquals(new BigDecimal("1200.00"), fields.netAmount());
    assertEquals(new BigDecimal("228.00"), fields.taxAmount());
    assertEquals(new BigDecimal("1428.00"), fields.grossAmount());
  }

  @Test
  void refusesExternalEntities() throws IOException {
    assertTrue(reader.read(InvoiceResource.of("xxe.xml").bytes()).isEmpty());
  }

  @Test
  void refusesEntityExpansion() throws IOException {
    assertTrue(reader.read(InvoiceResource.of("entity-expansion.xml")
      .bytes()).isEmpty());
  }

  @Test
  void ignoresOtherXml() {
    assertTrue(reader.read("<order><id>1</id></order>".getBytes())
      .isEmpty());
    assertTrue(reader.read("not xml".getBytes()).isEmpty());
  }

  private InvoiceFields read(String name) throws IOException {
    return reader.read(InvoiceResource.of(name).bytes()).orElseThrow();
  }
}