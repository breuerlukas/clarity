package de.lukasbreuer.clarity.api.transaction.recurring;

import de.lukasbreuer.clarity.api.transaction.Transaction;
import de.lukasbreuer.clarity.api.transaction.category.Category;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RecurringPaymentDetectorTest {
  private static final LocalDate TODAY = LocalDate.of(2026, 10, 12);

  @Test
  void detectsMonthlySubscriptionDespiteShiftedDebitDays() {
    var payments = RecurringPaymentDetector.create(TODAY).detect(List.of(
      debit("Netflix", "-13.99", "2026-07-01"),
      debit("Netflix", "-13.99", "2026-08-03"),
      debit("Netflix", "-13.99", "2026-09-01"),
      debit("Netflix", "-13.99", "2026-10-01")));
    assertEquals(1, payments.size());
    var netflix = payments.getFirst();
    assertEquals(RecurringCycle.MONTHLY, netflix.cycle());
    assertEquals(new BigDecimal("13.99"), netflix.amount());
    assertEquals(LocalDate.of(2026, 11, 1), netflix.nextDate());
  }

  @Test
  void groupsCardPaymentsWhoseReferenceChangesEveryMonth() {
    var payments = RecurringPaymentDetector.create(TODAY).detect(List.of(
      debit("Spotify P43438219D", "-6.99", "2026-05-11"),
      debit("SPOTIFY", "-6.99", "2026-06-09"),
      debit("Spotify P445538918", "-6.99", "2026-07-09"),
      debit("SPOTIFY", "-6.99", "2026-08-11"),
      debit("SPOTIFY*P47BDD5092", "-6.99", "2026-09-09")));
    assertEquals(1, payments.size());
    assertEquals("spotify", payments.getFirst().key());
    assertEquals(RecurringCycle.MONTHLY, payments.getFirst().cycle());
  }

  @Test
  void keepsNamesWithoutReferencesApart() {
    var payments = RecurringPaymentDetector.create(TODAY).detect(List.of(
      debit("PayPal *Netflix", "-13.99", "2026-08-01"),
      debit("PayPal *Netflix", "-13.99", "2026-09-01"),
      debit("PayPal *Netflix", "-13.99", "2026-10-01"),
      debit("PayPal *Disney", "-9.99", "2026-08-05"),
      debit("PayPal *Disney", "-9.99", "2026-09-05"),
      debit("PayPal *Disney", "-9.99", "2026-10-05")));
    assertEquals(2, payments.size());
  }

  @Test
  void ignoresSupermarketWithChangingAmounts() {
    var payments = RecurringPaymentDetector.create(TODAY).detect(List.of(
      debit("REWE", "-23.10", "2026-08-01"),
      debit("REWE", "-61.80", "2026-09-01"),
      debit("REWE", "-8.45", "2026-10-01")));
    assertTrue(payments.isEmpty());
  }

  @Test
  void keepsAStoppedSubscriptionAsMissing() {
    var payments = RecurringPaymentDetector.create(TODAY).detect(List.of(
      debit("Spotify", "-10.99", "2026-06-15"),
      debit("Spotify", "-10.99", "2026-07-15"),
      debit("Spotify", "-10.99", "2026-08-15")));
    assertEquals(RecurringStatus.MISSING,
      payments.getFirst().status(TODAY, Optional.empty()));
    assertEquals("spotify", payments.getFirst().key());
  }

  @Test
  void dropsSubscriptionACycleAfterItEnded() {
    var payments = RecurringPaymentDetector.create(TODAY).detect(List.of(
      debit("Spotify", "-10.99", "2026-04-15"),
      debit("Spotify", "-10.99", "2026-05-15"),
      debit("Spotify", "-10.99", "2026-06-15")));
    assertTrue(payments.isEmpty());
  }

  @Test
  void detectsYearlyInsuranceAndSpreadsItPerMonth() {
    var payments = RecurringPaymentDetector.create(TODAY).detect(List.of(
      debit("HUK-Coburg", "-240.00", "2025-01-10"),
      debit("HUK-Coburg", "-240.00", "2026-01-12")));
    assertEquals(RecurringCycle.YEARLY, payments.getFirst().cycle());
    assertEquals(new BigDecimal("20.00"), payments.getFirst().monthlyAmount());
  }

  @Test
  void toleratesSmallPriceChanges() {
    var payments = RecurringPaymentDetector.create(TODAY).detect(List.of(
      debit("Stadtwerke", "-80.00", "2026-08-05"),
      debit("Stadtwerke", "-80.00", "2026-09-05"),
      debit("Stadtwerke", "-86.00", "2026-10-05")));
    assertEquals(1, payments.size());
    assertEquals(new BigDecimal("80.00"), payments.getFirst().previousAmount());
  }

  private Transaction debit(String counterparty, String amount, String date) {
    return Transaction.create(UUID.randomUUID(), UUID.randomUUID(),
      UUID.randomUUID().toString(), LocalDate.parse(date),
      new BigDecimal(amount), "EUR", counterparty, null, "", Category.OTHER,
      false);
  }
}