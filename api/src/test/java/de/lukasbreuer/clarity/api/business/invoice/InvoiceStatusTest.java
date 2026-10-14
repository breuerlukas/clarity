package de.lukasbreuer.clarity.api.business.invoice;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class InvoiceStatusTest {
  private final InvoiceFixtures fixtures = InvoiceFixtures.create();

  @Test
  void showsOverdueAndAnnouncedInvoices() {
    var invoice = fixtures.outgoing("RE-9", "Kunde AG", "50.00");
    var today = fixtures.today();
    assertEquals(InvoiceStatus.OPEN, invoice.status(List.of(),
      today.minusDays(30)));
    invoice.announce(today, null);
    assertEquals(InvoiceStatus.ANNOUNCED, invoice.status(List.of(),
      today.minusDays(30)));
    assertEquals(InvoiceStatus.OVERDUE, invoice.status(List.of(), today));
  }
}