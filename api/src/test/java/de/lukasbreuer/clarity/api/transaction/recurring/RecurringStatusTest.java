package de.lukasbreuer.clarity.api.transaction.recurring;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class RecurringStatusTest {
  private static final LocalDate LAST = LocalDate.of(2026, 9, 1);

  @Test
  void goesMissingAfterTheCycleAndEndsACycleLater() {
    assertEquals(RecurringStatus.ACTIVE, status("2026-10-13"));
    assertEquals(RecurringStatus.MISSING, status("2026-10-14"));
    assertEquals(RecurringStatus.MISSING, status("2026-11-17"));
    assertEquals(RecurringStatus.ENDED, status("2026-11-18"));
  }

  @Test
  void endsWhenCancelledAndNothingIsDueBeforeTheEnd() {
    var payment = netflix();
    var today = LocalDate.parse("2026-09-10");
    assertEquals(RecurringStatus.ACTIVE, payment.status(today,
      setting("2026-10-15", null)));
    assertEquals(RecurringStatus.ENDED, payment.status(today,
      setting("2026-10-01", null)));
  }

  @Test
  void dismissalKeepsItRunningUntilItBooksAgain() {
    var today = LocalDate.parse("2026-11-30");
    assertEquals(RecurringStatus.ACTIVE, netflix().status(today,
      setting(null, "2026-09-01")));
    assertEquals(RecurringStatus.ENDED, netflix().status(today,
      setting(null, "2026-08-01")));
  }

  @Test
  void marksBookingsTheOwnerSaidAreNoFixedCost() {
    var setting = setting(null, null).orElseThrow();
    var today = LocalDate.parse("2026-09-10");
    assertEquals(false, netflix().information(today, Optional.of(setting))
      .get("ignored"));
    setting.change(null, null, null, true);
    assertEquals(true, netflix().information(today, Optional.of(setting))
      .get("ignored"));
  }

  @Test
  void showsTheOwnersNameInsteadOfTheOneFromTheBookings() {
    var setting = setting(null, null).orElseThrow();
    var today = LocalDate.parse("2026-09-10");
    assertEquals(null, netflix().information(today, Optional.of(setting))
      .get("name"));
    setting.rename("Netflix Standard");
    assertEquals("Netflix Standard", netflix().information(today,
      Optional.of(setting)).get("name"));
  }

  private RecurringStatus status(String today) {
    return netflix().status(LocalDate.parse(today), Optional.empty());
  }

  private RecurringPayment netflix() {
    return RecurringPayment.create("netflix", "Netflix", "SUBSCRIPTIONS",
      new BigDecimal("13.99"), new BigDecimal("13.99"), "EUR",
      RecurringCycle.MONTHLY, LAST);
  }

  private Optional<RecurringSetting> setting(
    String cancelledOn, String dismissedLastDate
  ) {
    return Optional.of(RecurringSetting.create(UUID.randomUUID(),
      UUID.randomUUID(), "netflix",
      cancelledOn == null ? null : LocalDate.parse(cancelledOn),
      dismissedLastDate == null ? null : LocalDate.parse(dismissedLastDate),
      null, null, null));
  }
}
