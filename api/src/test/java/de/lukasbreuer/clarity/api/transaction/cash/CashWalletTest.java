package de.lukasbreuer.clarity.api.transaction.cash;

import de.lukasbreuer.clarity.api.transaction.Transaction;
import de.lukasbreuer.clarity.api.transaction.category.Category;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CashWalletTest {
  private static final UUID BANK_ACCOUNT = UUID.randomUUID();
  private static final CashWallet WALLET = CashWallet.create(UUID.randomUUID(),
    UUID.randomUUID(), LocalDate.of(2026, 10, 1));

  @Test
  void withdrawalsFillTheWalletAndEntriesChangeIt() {
    var balance = WALLET.balance(List.of(
      bank("-100.00", Category.CASH, "2026-10-02"),
      bank("-40.00", "GROCERIES", "2026-10-03"),
      WALLET.entry(LocalDate.of(2026, 10, 4), new BigDecimal("-12.50"),
        "Bäckerei", "", "DINING"),
      WALLET.entry(LocalDate.of(2026, 10, 5), new BigDecimal("20.00"),
        "Flohmarkt", "", Category.INCOME)));
    assertEquals(0, new BigDecimal("107.50").compareTo(balance));
  }

  @Test
  void withdrawalsBeforeTheStartAreLongSpent() {
    var balance = WALLET.balance(List.of(
      bank("-200.00", Category.CASH, "2026-09-30")));
    assertEquals(0, BigDecimal.ZERO.compareTo(balance));
  }

  @Test
  void payingCashInAtTheBankEmptiesTheWallet() {
    var balance = WALLET.balance(List.of(
      bank("-100.00", Category.CASH, "2026-10-02"),
      bank("60.00", Category.CASH, "2026-10-08")));
    assertEquals(0, new BigDecimal("40.00").compareTo(balance));
  }

  @Test
  void entriesAreBookedOnTheWalletAndCountAsPickedByHand() {
    var entry = WALLET.entry(LocalDate.of(2026, 10, 4),
      new BigDecimal("-3.20"), "Kiosk", "Zeitung", "OTHER");
    assertTrue(WALLET.holds(entry));
    assertTrue(entry.manuallyCategorized());
  }

  private Transaction bank(String amount, String category, String date) {
    return Transaction.create(UUID.randomUUID(), BANK_ACCOUNT, "reference",
      LocalDate.parse(date), new BigDecimal(amount), "EUR", "Sparkasse", null,
      "", category, false);
  }
}