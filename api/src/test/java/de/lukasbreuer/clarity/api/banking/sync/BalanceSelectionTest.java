package de.lukasbreuer.clarity.api.banking.sync;

import de.lukasbreuer.clarity.api.banking.account.BankAccount;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class BalanceSelectionTest {
  private final BalanceSelection balances = BalanceSelection.of(new JSONArray(
    """
    [{"balance_type": "CLBD",
      "balance_amount": {"amount": "10.00", "currency": "EUR"}},
     {"balance_type": "ITAV",
      "balance_amount": {"amount": "86.40", "currency": "EUR"}}]
    """));

  @Test
  void prefersTodaysAvailableBalance() {
    assertEquals(new BigDecimal("86.40"), balances.select().orElseThrow());
    assertEquals("EUR", balances.currency().orElseThrow());
  }

  @Test
  void givesAnAccountWithoutCurrencyTheOneOfItsBalance() {
    var paypal = account("XXX");
    paypal.adoptBalanceCurrency("EUR");
    assertEquals("EUR", paypal.currency());
    var giro = account("EUR");
    giro.adoptBalanceCurrency("USD");
    assertEquals("EUR", giro.currency());
  }

  private BankAccount account(String currency) {
    return BankAccount.create(UUID.randomUUID(), UUID.randomUUID(), "uid",
      null, "PayPal", currency, null, 0, 0, null, null);
  }
}
