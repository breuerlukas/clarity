package de.lukasbreuer.clarity.api.business.invoice.matching;

import de.lukasbreuer.clarity.api.business.invoice.Invoice;
import de.lukasbreuer.clarity.api.business.invoice.InvoiceFixtures;
import de.lukasbreuer.clarity.api.business.invoice.InvoicePayment;
import de.lukasbreuer.clarity.api.business.invoice.InvoiceStatus;
import de.lukasbreuer.clarity.api.transaction.Transaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PaymentMatchingTest {
  private final InvoiceFixtures fixtures = InvoiceFixtures.create();

  @Test
  void paysInvoiceNamedInReference() {
    var invoice = fixtures.outgoing("RE-2026-042", "Müller Logistik GmbH",
      "1428.00");
    var transaction = fixtures.booking("1428.00", "Mueller Logistik",
      "Rechnung RE 2026 042 vom 01.10.");
    var result = match(List.of(invoice), List.of(transaction), List.of());
    assertEquals(1, result.links().size());
    assertEquals(new BigDecimal("1428.00"), result.links().getFirst()
      .amount());
    assertEquals(InvoiceStatus.PAID, invoice.status(result.links(),
      fixtures.today()));
  }

  @Test
  void paysPartOfInvoice() {
    var invoice = fixtures.outgoing("RE-2026-043", "Müller Logistik GmbH",
      "1000.00");
    var transaction = fixtures.booking("400.00", "Müller",
      "Teilzahlung RE-2026-043");
    var result = match(List.of(invoice), List.of(transaction), List.of());
    assertEquals(new BigDecimal("400.00"), result.links().getFirst()
      .amount());
    assertEquals(new BigDecimal("600.00"), invoice.openAmount(
      result.links()));
    assertEquals(InvoiceStatus.PARTIAL, invoice.status(result.links(),
      fixtures.today().minusDays(10)));
  }

  @Test
  void paysSeveralInvoicesWithOneTransfer() {
    var first = fixtures.outgoing("RE-2026-050", "Kunde AG", "100.00");
    var second = fixtures.outgoing("RE-2026-051", "Kunde AG", "250.00");
    var transaction = fixtures.booking("350.00", "Kunde AG",
      "RE-2026-050, RE-2026-051");
    var result = match(List.of(first, second), List.of(transaction),
      List.of());
    assertEquals(2, result.links().size());
    assertTrue(first.isPaid(result.links()));
    assertTrue(second.isPaid(result.links()));
  }

  @Test
  void settlesInvoiceWithEarlyPaymentDiscount() {
    var invoice = fixtures.incoming("2026-1187", "Druckerei Weber", "297.50");
    var transaction = fixtures.booking("-291.55", "Druckerei Weber",
      "2026-1187 abzgl. 2% Skonto");
    var result = match(List.of(invoice), List.of(transaction), List.of());
    assertEquals(List.of(invoice), result.settled());
    assertTrue(invoice.settled());
    assertEquals(InvoiceStatus.PAID, invoice.status(result.links(),
      fixtures.today()));
  }

  @Test
  void paysExactAmountFromKnownIbanWithoutNumber() {
    var invoice = fixtures.incoming("A-77", "Nordlicht Hosting GmbH",
      "119.00");
    var transaction = fixtures.booking("-119.0000", "NORDLICHT",
      "DE89 3704 0044 0532 0130 00", "Lastschrift");
    var result = match(List.of(invoice), List.of(transaction), List.of());
    assertEquals(1, result.links().size());
  }

  @Test
  void neverLinksAgainWhatTheOwnerRemoved() {
    var invoice = fixtures.outgoing("RE-2026-060", "Kunde AG", "80.00");
    var transaction = fixtures.booking("80.00", "Kunde AG", "RE-2026-060");
    var removed = InvoicePayment.create(UUID.randomUUID(), invoice.id(),
      transaction.id(), fixtures.account(), new BigDecimal("80.00"),
      fixtures.today(), true, true, 0);
    assertTrue(match(List.of(invoice), List.of(transaction),
      List.of(removed)).links().isEmpty());
  }

  @Test
  void ignoresWrongDirectionAndExcludedBookings() {
    var invoice = fixtures.outgoing("RE-2026-070", "Kunde AG", "80.00");
    var refund = fixtures.booking("-80.00", "Kunde AG", "RE-2026-070");
    var withdrawal = fixtures.booking("80.00", "Kunde AG", "RE-2026-070");
    var result = PaymentMatching.create(List.of(invoice), List.of())
      .match(List.of(refund, withdrawal), Set.of(withdrawal.id()));
    assertTrue(result.links().isEmpty());
  }

  private MatchResult match(
    List<Invoice> invoices, List<Transaction> transactions,
    List<InvoicePayment> payments
  ) {
    return PaymentMatching.create(invoices, payments)
      .match(transactions, Set.of());
  }
}