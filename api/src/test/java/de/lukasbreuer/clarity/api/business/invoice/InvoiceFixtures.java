package de.lukasbreuer.clarity.api.business.invoice;

import de.lukasbreuer.clarity.api.transaction.Category;
import de.lukasbreuer.clarity.api.transaction.Transaction;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Invoices and bookings of one business account, dated around a fixed day,
 * for the invoice and matching tests.
 */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(staticName = "create")
public final class InvoiceFixtures {
  private final LocalDate today = LocalDate.of(2026, 10, 13);
  private final UUID account = UUID.randomUUID();

  public Invoice outgoing(String number, String customer, String gross) {
    return invoice(InvoiceDirection.OUTGOING, number, "Breuer Software UG",
      customer, null, gross);
  }

  public Invoice incoming(String number, String issuer, String gross) {
    return invoice(InvoiceDirection.INCOMING, number, issuer,
      "Breuer Software UG", "DE89370400440532013000", gross);
  }

  public Transaction booking(
    String amount, String counterparty, String description
  ) {
    return booking(amount, counterparty, null, description);
  }

  public Transaction booking(
    String amount, String counterparty, String iban, String description
  ) {
    return Transaction.create(UUID.randomUUID(), account,
      UUID.randomUUID().toString(), today, new BigDecimal(amount), "EUR",
      counterparty, iban, description, Category.OTHER, false);
  }

  private Invoice invoice(
    InvoiceDirection direction, String number, String issuer,
    String recipient, String iban, String gross
  ) {
    return Invoice.create(UUID.randomUUID(), UUID.randomUUID(), direction,
      number, issuer, recipient, iban, "EUR", null, null,
      new BigDecimal(gross), today.minusDays(20), today.minusDays(6),
      InvoiceFormat.XRECHNUNG, false, false, null, null, null, null, null,
      null, null, null, null, null, 0);
  }
}