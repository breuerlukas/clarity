package de.lukasbreuer.clarity.api.banking.sync;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EnableBankingTransactionTest {
  private static final String DEBIT = """
    {"entry_reference": "ref-1", "status": "BOOK",
     "credit_debit_indicator": "DBIT", "booking_date": "2026-10-01",
     "transaction_amount": {"amount": "13.99", "currency": "EUR"},
     "creditor": {"name": " Netflix International B.V. "},
     "creditor_account": {"iban": "NL00TEST0123456789"},
     "debtor": {"name": "Lukas"},
     "remittance_information": ["Monthly plan", "Oct"]}
    """;

  private static final String CREDIT_WITHOUT_REFERENCE = """
    {"credit_debit_indicator": "CRDT", "value_date": "2026-10-02",
     "transaction_amount": {"amount": "2500.00", "currency": "EUR"},
     "debtor": {"name": "Firma GmbH"}}
    """;

  @Test
  void readsDebitWithSignAndCreditorAsCounterparty() {
    var transaction = EnableBankingTransaction.of(new JSONObject(DEBIT));
    assertEquals(new BigDecimal("-13.99"), transaction.amount());
    assertEquals("Netflix International B.V.", transaction.counterparty());
    assertEquals("NL00TEST0123456789", transaction.counterpartyIban());
    assertEquals("Monthly plan Oct", transaction.description());
    assertEquals(LocalDate.of(2026, 10, 1), transaction.bookingDate());
    assertEquals("ref-1", transaction.externalId());
  }

  @Test
  void readsCreditAndFallsBackToValueDateAndContentHash() {
    var transaction = EnableBankingTransaction.of(
      new JSONObject(CREDIT_WITHOUT_REFERENCE));
    assertEquals(new BigDecimal("2500.00"), transaction.amount());
    assertEquals("Firma GmbH", transaction.counterparty());
    assertEquals(LocalDate.of(2026, 10, 2), transaction.bookingDate());
    assertTrue(transaction.externalId().startsWith("hash:"));
    assertEquals(transaction.externalId(), EnableBankingTransaction.of(
      new JSONObject(CREDIT_WITHOUT_REFERENCE)).externalId());
  }

  @Test
  void skipsPendingTransactions() {
    var pending = new JSONObject(DEBIT).put("status", "PDNG");
    assertFalse(EnableBankingTransaction.of(pending).isBooked());
  }
}