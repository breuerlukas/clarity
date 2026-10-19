package de.lukasbreuer.clarity.api.transaction.receipt;

import de.lukasbreuer.clarity.api.transaction.Transaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ReceiptMatchingTest {
  private static final UUID BANK_ACCOUNT = UUID.randomUUID();
  private static final UUID WALLET = UUID.randomUUID();
  private static final Receipt RECEIPT = Receipt.create(UUID.randomUUID(),
    UUID.randomUUID(), null, "REWE", LocalDate.of(2026, 10, 17),
    new BigDecimal("23.47"), List.of(), "receipt.jpg", "image/jpeg", 1, 0);

  @Test
  void suggestsTheClosestCardPaymentOverTheTotalFirst() {
    var later = payment(BANK_ACCOUNT, "-23.47", "2026-10-21");
    var closer = payment(BANK_ACCOUNT, "-23.47", "2026-10-18");
    var suggestions = ReceiptMatching.of(RECEIPT).suggestions(List.of(
      later, closer, payment(BANK_ACCOUNT, "-23.40", "2026-10-17"),
      payment(BANK_ACCOUNT, "-23.47", "2026-10-25")), WALLET, Set.of());
    assertEquals(List.of(closer, later), suggestions);
  }

  @Test
  void skipsCashEntriesAndPaymentsWithAReceipt() {
    var cash = payment(WALLET, "-23.47", "2026-10-17");
    var taken = payment(BANK_ACCOUNT, "-23.47", "2026-10-17");
    assertTrue(ReceiptMatching.of(RECEIPT).suggestions(List.of(cash, taken),
      WALLET, Set.of(taken.id())).isEmpty());
  }

  private Transaction payment(UUID account, String amount, String date) {
    return Transaction.create(UUID.randomUUID(), account, "reference",
      LocalDate.parse(date), new BigDecimal(amount), "EUR", "REWE", null, "",
      "GROCERIES", false);
  }
}