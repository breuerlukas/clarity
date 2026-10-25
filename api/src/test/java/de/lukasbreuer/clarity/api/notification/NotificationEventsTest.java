package de.lukasbreuer.clarity.api.notification;

import de.lukasbreuer.clarity.api.banking.account.BankAccount;
import de.lukasbreuer.clarity.api.banking.connection.BankConnection;
import de.lukasbreuer.clarity.api.transaction.recurring.RecurringCycle;
import de.lukasbreuer.clarity.api.transaction.recurring.RecurringPayment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class NotificationEventsTest {
  private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");
  private final Instant now = Instant.now();
  private final NotificationEvents events = NotificationEvents.create(now,
    ZONE);

  @Test
  void announcesConsentsExpiringOrExpiredWithinAWeek() {
    var kinds = events.consents(List.of(connection(Duration.ofDays(6)),
        connection(Duration.ofDays(8)), connection(Duration.ofDays(-2)),
        connection(Duration.ofDays(-9))))
      .stream().map(Notification::kind).toList();
    assertEquals(List.of(NotificationKind.CONSENT_EXPIRING,
      NotificationKind.CONSENT_EXPIRED), kinds);
  }

  @Test
  void keysAConsentByItsExpirySoARenewalIsAnnouncedAgain() {
    var first = connection(Duration.ofDays(3));
    var key = events.consents(List.of(first)).getFirst().eventKey();
    assertEquals("CONSENT_EXPIRING:" + first.id() + ":" + first.validUntil(),
      key);
  }

  @Test
  void announcesSyncsFailingForADayOnValidConsents() {
    var valid = connection(Duration.ofDays(30));
    var expired = connection(Duration.ofDays(-1));
    var failing = account(valid, Duration.ofHours(30), "status_500");
    var accounts = List.of(failing,
      account(valid, Duration.ofHours(10), "status_500"),
      account(valid, Duration.ofHours(30), null),
      account(expired, Duration.ofHours(30), "consent"));
    var notifications = events.failingSyncs(accounts,
      Map.of(valid.id(), valid, expired.id(), expired));
    assertEquals(1, notifications.size());
    assertEquals("SYNC_FAILING:" + failing.id() + ":" +
      failing.lastSyncedAt(), notifications.getFirst().eventKey());
  }

  @Test
  void announcesRecentPriceChangesOnly() {
    var today = LocalDate.ofInstant(now, ZONE);
    var notifications = events.priceChanges(UUID.randomUUID(), List.of(
      payment("Netflix", "15.99", "13.99", today.minusDays(2)),
      payment("Spotify", "12.99", "10.99", today.minusDays(20)),
      payment("Telekom", "39.95", "39.95", today.minusDays(1))),
      Map.of("netflix", "Netflix Standard"));
    assertEquals(1, notifications.size());
    assertEquals("Netflix Standard",
      notifications.getFirst().values().get("counterparty"));
  }

  private BankConnection connection(Duration validFor) {
    return BankConnection.create(UUID.randomUUID(), "session", "DKB", "DE",
      now.plus(validFor).toEpochMilli(), 0, UUID.randomUUID());
  }

  private BankAccount account(
    BankConnection connection, Duration sinceSync, String failure
  ) {
    return BankAccount.create(UUID.randomUUID(), connection.id(), "uid",
      null, "Giro", "EUR", BigDecimal.ZERO,
      now.minus(sinceSync).toEpochMilli(), now.toEpochMilli(), failure, null);
  }

  private RecurringPayment payment(
    String counterparty, String amount, String previous, LocalDate lastDate
  ) {
    return RecurringPayment.create(counterparty.toLowerCase(Locale.ROOT),
      counterparty, "SUBSCRIPTIONS", new BigDecimal(amount),
      new BigDecimal(previous), "EUR", RecurringCycle.MONTHLY, lastDate);
  }
}
