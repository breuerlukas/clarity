import { describe, expect, it } from "vitest";
import type { RecurringPayment, Transaction } from "@/lib/api";
import { cancellationReminders, recurringBookings, summarizeMonth, total, upcomingThisMonth, yearly } from "@/lib/overview";
import { monthRange, shiftMonth } from "@/lib/dates";

const transaction = (
  amount: number,
  category: Transaction["category"],
): Transaction => ({
  id: crypto.randomUUID(),
  account: "a",
  date: "2026-10-01",
  amount,
  currency: "EUR",
  counterparty: "x",
  description: "",
  category,
});

describe("summarizeMonth", () => {
  it("sums income and spending in cents and leaves transfers out", () => {
    const summary = summarizeMonth([
      transaction(2500, "SALARY"),
      transaction(-0.1, "GROCERIES"),
      transaction(-0.2, "GROCERIES"),
      transaction(-13.99, "SUBSCRIPTIONS"),
      transaction(-500, "TRANSFER"),
      transaction(500, "TRANSFER"),
    ]);
    expect(summary.income).toBe(2500);
    expect(summary.spending).toBe(14.29);
    expect(summary.categories).toEqual([
      { category: "SUBSCRIPTIONS", amount: 13.99 },
      { category: "GROCERIES", amount: 0.3 },
    ]);
  });
});

describe("upcomingThisMonth", () => {
  it("keeps only payments due between today and the end of the month", () => {
    const payment = (next_date: string) =>
      ({ next_date, amount: 10 }) as RecurringPayment;
    const upcoming = upcomingThisMonth(
      [payment("2026-10-11"), payment("2026-10-12"), payment("2026-10-31"),
        payment("2026-11-01")],
      "2026-10-12",
      "2026-10",
    );
    expect(upcoming.map((item) => item.next_date)).toEqual([
      "2026-10-12",
      "2026-10-31",
    ]);
  });
});

describe("dates", () => {
  it("knows month lengths and crosses year boundaries", () => {
    expect(monthRange("2028-02")).toEqual({ from: "2028-02-01", to: "2028-02-29" });
    expect(shiftMonth("2026-01", -1)).toBe("2025-12");
    expect(total([0.1, 0.2])).toBe(0.3);
  });
});

describe("yearly", () => {
  it("multiplies the amount itself, not the rounded monthly share", () => {
    const payment = (amount: number, cycle: RecurringPayment["cycle"]) =>
      ({ amount, cycle, monthly_amount: Math.round((amount / 12) * 100) / 100 }) as RecurringPayment;
    expect(yearly(payment(289.4, "YEARLY"))).toBe(289.4);
    expect(yearly(payment(55.08, "QUARTERLY"))).toBeCloseTo(220.32);
    expect(yearly(payment(10.99, "MONTHLY"))).toBeCloseTo(131.88);
  });
});

describe("cancellationReminders", () => {
  const payment = (key: string, cancel_by: string | null, extra: Partial<RecurringPayment> = {}) =>
    ({ key, cancel_by, status: "ACTIVE", cancelled_on: null, ...extra }) as RecurringPayment;

  it("reminds of running ones whose last day to cancel is within two weeks", () => {
    const reminders = cancellationReminders(
      [
        payment("late", "2026-10-26"),
        payment("soon", "2026-10-13"),
        payment("past", "2026-10-11"),
        payment("far", "2026-10-27"),
        payment("none", null),
        payment("cancelled", "2026-10-14", { cancelled_on: "2026-11-01" }),
        payment("ended", "2026-10-14", { status: "ENDED" }),
      ],
      "2026-10-12",
    );
    expect(reminders.map((entry) => entry.key)).toEqual(["soon", "late"]);
  });
});

describe("recurringBookings", () => {
  const booking = (date: string, amount: number, counterparty: string, description = "", category = "SUBSCRIPTIONS") =>
    ({ date, amount, counterparty, description, category }) as Transaction;

  it("finds the debits of a fixed cost by its key, newest first", () => {
    const bookings = recurringBookings({ key: "netflix international b.v." } as RecurringPayment, [
      booking("2026-08-03", -13.99, "Netflix  International B.V."),
      booking("2026-10-03", -15.99, "NETFLIX International B.V. "),
      booking("2026-09-05", 13.99, "Netflix International B.V."),
      booking("2026-09-03", -13.99, "Netflix International B.V.", "", "TRANSFER"),
      booking("2026-09-04", -9.99, "Spotify AB"),
    ]);
    expect(bookings.map((entry) => entry.date)).toEqual(["2026-10-03", "2026-08-03"]);
  });

  it("falls back to the purpose without a counterparty", () => {
    const bookings = recurringBookings({ key: "miete whg. 3" } as RecurringPayment, [booking("2026-10-01", -1050, " ", "Miete Whg. 3")]);
    expect(bookings).toHaveLength(1);
  });
});
