package de.lukasbreuer.clarity.api.banking.account;

import de.lukasbreuer.clarity.api.banking.connection.BankConnection;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BankAccountTest {
  private final UUID home = UUID.randomUUID();
  private final UUID household = UUID.randomUUID();
  private final BankConnection connection = BankConnection.create(
    UUID.randomUUID(), "session", "Santander", "DE", 0, 0, home);
  private final BankAccount account = BankAccount.create(UUID.randomUUID(),
    connection.id(), "uid", null, "Giro", "EUR", null, 0, 0, null, null,
    null, null);

  @Test
  void followsItsConnectionUntilMovedAndAgainWhenMovedBack() {
    assertEquals(home, account.spaceIn(connection));
    account.moveTo(household, connection);
    assertEquals(household, account.spaceIn(connection));
    account.moveTo(home, connection);
    assertEquals(home, account.spaceIn(connection));
    assertNull(account.spaceId());
  }

  @Test
  void hidesAndShowsAgain() {
    assertFalse(account.isHidden());
    account.hide(true);
    assertTrue(account.isHidden());
    account.hide(false);
    assertFalse(account.isHidden());
  }
}
