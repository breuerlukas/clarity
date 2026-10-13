package de.lukasbreuer.clarity.api.business.invoice.matching;

import de.lukasbreuer.clarity.api.business.invoice.Invoice;
import de.lukasbreuer.clarity.api.business.invoice.InvoiceDirection;
import de.lukasbreuer.clarity.api.business.invoice.InvoiceFormat;
import de.lukasbreuer.clarity.api.business.invoice.InvoicePayment;
import de.lukasbreuer.clarity.api.business.invoice.InvoiceStatus;
import de.lukasbreuer.clarity.api.transaction.Category;
import de.lukasbreuer.clarity.api.transaction.Transaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PaymentMatchingTest {
  private static final LocalDate TODAY = LocalDate.of(2026, 10, 13);
  private static final UUID ACCOUNT = UUID.randomUUID();

  @Test
  void paysInvoiceNamedInReference() {
    var invoice = outgoing("RE-2026-042", "Müller Logistik GmbH", "1428.00");
    var transaction = booking("1428.00", "Mueller Logistik",
      "Rechnung RE 2026 042 vom 01.10.");
    var result = match(List.of(invoice), List.of(transaction), List.of());
    assertEquals(1, result.links().size());
    assertEquals(new BigDecimal("1428.00"), result.links().getFirst()
      .amount());
    assertEquals(InvoiceStatus.PAID, invoice.status(result.links(), TODAY));
  }

  @Test
  void paysPartOfInvoice() {
    var invoice = outgoing("RE-2026-043", "Müller Logistik GmbH", "1000.00");
    var transaction = booking("400.00", "Müller", "Teilzahlung RE-2026-043");
    var result = match(List.of(invoice), List.of(transaction), List.of());
    assertEquals(new BigDecimal("400.00"), result.links().getFirst()
      .amount());
    assertEquals(new BigDecimal("600.00"), invoice.openAmount(
      result.links()));
    assertEquals(InvoiceStatus.PARTIAL, invoice.status(result.links(),
      TODAY.minusDays(10)));
  }

  @Test
  void paysSeveralInvoicesWithOneTransfer() {
    var first = outgoing("RE-2026-050", "Kunde AG", "100.00");
    var second = outgoing("RE-2026-051", "Kunde AG", "250.00");
    var transaction = booking("350.00", "Kunde AG",
      "RE-2026-050, RE-2026-051");
    var result = match(List.of(first, second), List.of(transaction),
      List.of());
    assertEquals(2, result.links().size());
    assertTrue(first.isPaid(result.links()));
    assertTrue(second.isPaid(result.links()));
  }

  @Test
  void settlesInvoiceWithEarlyPaymentDiscount() {
    var invoice = incoming("2026-1187", "Druckerei Weber", "297.50");
    var transaction = booking("-291.55", "Druckerei Weber",
      "2026-1187 abzgl. 2% Skonto");
    var result = match(List.of(invoice), List.of(transaction), List.of());
    assertEquals(List.of(invoice), result.settled());
    assertTrue(invoice.settled());
    assertEquals(InvoiceStatus.PAID, invoice.status(result.links(), TODAY));
  }

  @Test
  void paysExactAmountFromKnownIbanWithoutNumber() {
    var invoice = incoming("A-77", "Nordlicht Hosting GmbH", "119.00");
    var transaction = Transaction.create(UUID.randomUUID(), ACCOUNT, "x",
      TODAY, new BigDecimal("-119.0000"), "EUR", "NORDLICHT",
      "DE89 3704 0044 0532 0130 00", "Lastschrift", Category.OTHER, false);
    var result = match(List.of(invoice), List.of(transaction), List.of());
    assertEquals(1, result.links().size());
  }

  @Test
  void leavesAmbiguousPaymentsToTheOwner() {
    var first = outgoing("RE-1", "Kunde AG", "500.00");
    var second = outgoing("RE-2", "Kunde AG", "500.00");
    var transaction = booking("500.00", "Kunde AG", "Danke");
    var result = match(List.of(first, second), List.of(transaction),
      List.of());
    assertTrue(result.links().isEmpty());
    var suggestions = PaymentMatching.suggest(first, List.of(transaction),
      List.of(), Set.of());
    assertEquals(Set.of(MatchReason.AMOUNT, MatchReason.NAME),
      suggestions.getFirst().reasons());
  }

  @Test
  void neverLinksAgainWhatTheOwnerRemoved() {
    var invoice = outgoing("RE-2026-060", "Kunde AG", "80.00");
    var transaction = booking("80.00", "Kunde AG", "RE-2026-060");
    var removed = InvoicePayment.create(UUID.randomUUID(), invoice.id(),
      transaction.id(), ACCOUNT, new BigDecimal("80.00"), TODAY, true, true,
      0);
    assertTrue(match(List.of(invoice), List.of(transaction),
      List.of(removed)).links().isEmpty());
  }

  @Test
  void ignoresWrongDirectionAndExcludedBookings() {
    var invoice = outgoing("RE-2026-070", "Kunde AG", "80.00");
    var refund = booking("-80.00", "Kunde AG", "RE-2026-070");
    var withdrawal = booking("80.00", "Kunde AG", "RE-2026-070");
    var result = PaymentMatching.match(List.of(invoice),
      List.of(refund, withdrawal), List.of(), Set.of(withdrawal.id()));
    assertTrue(result.links().isEmpty());
  }

  @Test
  void findsNumbersInAnyForm() {
    assertTrue(InvoiceNumber.appearsIn("RE-2026-042", "re2026042"));
    assertTrue(InvoiceNumber.appearsIn("1042", "Rechnung 1042 danke"));
    assertTrue(InvoiceNumber.appearsIn("1042", "RE1042"));
    assertFalse(InvoiceNumber.appearsIn("104", "DE89104200 Betrag 1042"));
    assertFalse(InvoiceNumber.appearsIn("12", "12"));
  }

  @Test
  void comparesCompanyNames() {
    assertTrue(CompanyName.isSimilar("Müller Logistik GmbH",
      "MUELLER LOGISTIK"));
    assertTrue(CompanyName.isSimilar("Nordlicht Hosting GmbH", "NORDLICHT"));
    assertFalse(CompanyName.isSimilar("Kunde GmbH & Co. KG", "Andere GmbH"));
  }

  @Test
  void showsOverdueAndAnnouncedInvoices() {
    var invoice = outgoing("RE-9", "Kunde AG", "50.00");
    assertEquals(InvoiceStatus.OPEN, invoice.status(List.of(),
      TODAY.minusDays(30)));
    invoice.announce(TODAY, null);
    assertEquals(InvoiceStatus.ANNOUNCED, invoice.status(List.of(),
      TODAY.minusDays(30)));
    assertEquals(InvoiceStatus.OVERDUE, invoice.status(List.of(), TODAY));
  }

  private PaymentMatching.Result match(
    List<Invoice> invoices, List<Transaction> transactions,
    List<InvoicePayment> payments
  ) {
    return PaymentMatching.match(invoices, transactions,
      new ArrayList<>(payments), Set.of());
  }

  private Invoice outgoing(String number, String customer, String gross) {
    return invoice(InvoiceDirection.OUTGOING, number, "Breuer Software UG",
      customer, null, gross);
  }

  private Invoice incoming(String number, String issuer, String gross) {
    return invoice(InvoiceDirection.INCOMING, number, issuer,
      "Breuer Software UG", "DE89370400440532013000", gross);
  }

  private Invoice invoice(
    InvoiceDirection direction, String number, String issuer,
    String recipient, String iban, String gross
  ) {
    return Invoice.create(UUID.randomUUID(), UUID.randomUUID(), direction,
      number, issuer, recipient, iban, "EUR", null, null,
      new BigDecimal(gross), TODAY.minusDays(20), TODAY.minusDays(6),
      InvoiceFormat.XRECHNUNG, false, false, null, null, null, null, null,
      null, null, null, null, null, 0);
  }

  private Transaction booking(
    String amount, String counterparty, String description
  ) {
    return Transaction.create(UUID.randomUUID(), ACCOUNT,
      UUID.randomUUID().toString(), TODAY, new BigDecimal(amount), "EUR",
      counterparty, null, description, Category.OTHER, false);
  }
}
