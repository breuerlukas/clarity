import { describe, expect, it } from "vitest";
import type { RecurringPayment, Transaction } from "@/lib/api";
import { summarizeMonth, total, upcomingThisMonth, yearly } from "@/lib/overview";
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
