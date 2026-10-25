package de.lukasbreuer.clarity.api.notification;

import de.lukasbreuer.clarity.api.banking.account.BankAccount;
import de.lukasbreuer.clarity.api.banking.connection.BankConnection;
import de.lukasbreuer.clarity.api.transaction.recurring.RecurringPayment;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Finds what is worth telling right now. Only recent events count, so
 * switching notifications on doesn't send a mail for every price change of
 * the past year: a consent expiring within a week or expired within one, a
 * sync failing for a day, a price changed with a booking of the last week.
 */
@RequiredArgsConstructor(staticName = "create")
public final class NotificationEvents {
  private static final Duration WINDOW = Duration.ofDays(7);
  private static final Duration FAILING_FOR = Duration.ofDays(1);

  private final Instant now;
  private final ZoneId zone;

  public List<Notification> consents(List<BankConnection> connections) {
    return connections.stream().map(this::consent).flatMap(Optional::stream)
      .toList();
  }

  private Optional<Notification> consent(BankConnection connection) {
    var until = Instant.ofEpochMilli(connection.validUntil());
    var values = Map.<String, Object>of("bank", connection.bankName(),
      "date", date(until));
    if (until.isAfter(now)) {
      return Optional.of(until).filter(now.plus(WINDOW)::isAfter)
        .map(_ -> event(NotificationKind.CONSENT_EXPIRING, connection, values));
    }
    return Optional.of(until).filter(now.minus(WINDOW)::isBefore)
      .map(_ -> event(NotificationKind.CONSENT_EXPIRED, connection, values));
  }

  private Notification event(
    NotificationKind kind, BankConnection connection,
    Map<String, Object> values
  ) {
    return Notification.create(kind, kind + ":" + connection.id() + ":" +
      connection.validUntil(), values);
  }

  /**
   * Accounts of expired consents fail as well, but are announced as such.
   */
  public List<Notification> failingSyncs(
    List<BankAccount> accounts, Map<UUID, BankConnection> connections
  ) {
    return accounts.stream()
      .filter(account -> account.syncFailure() != null)
      .filter(account -> now.minus(FAILING_FOR)
        .isAfter(Instant.ofEpochMilli(account.lastSyncedAt())))
      .filter(account -> connections.containsKey(account.connectionId()))
      .filter(account -> connections.get(account.connectionId()).isValid())
      .map(account -> failingSync(account,
        connections.get(account.connectionId())))
      .toList();
  }

  private Notification failingSync(
    BankAccount account, BankConnection connection
  ) {
    var name = account.name() == null ? connection.bankName() : account.name();
    return Notification.create(NotificationKind.SYNC_FAILING,
      "SYNC_FAILING:" + account.id() + ":" + account.lastSyncedAt(),
      Map.of("account", name, "bank", connection.bankName()));
  }

  public List<Notification> priceChanges(
    UUID spaceId, List<RecurringPayment> payments
  ) {
    var since = date(now.minus(WINDOW));
    return payments.stream()
      .filter(payment -> payment.amount().compareTo(
        payment.previousAmount()) != 0)
      .filter(payment -> !payment.lastDate().isBefore(since))
      .map(payment -> priceChange(spaceId, payment))
      .toList();
  }

  private Notification priceChange(UUID spaceId, RecurringPayment payment) {
    return Notification.create(NotificationKind.PRICE_CHANGED,
      "PRICE_CHANGED:" + spaceId + ":" +
        payment.counterparty().toLowerCase(Locale.ROOT) + ":" +
        payment.lastDate(),
      Map.of("counterparty", payment.counterparty(),
        "previous", payment.previousAmount(), "amount", payment.amount(),
        "currency", payment.currency()));
  }

  private LocalDate date(Instant instant) {
    return LocalDate.ofInstant(instant, zone);
  }
}
