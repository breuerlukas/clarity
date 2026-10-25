import { describe, expect, it } from "vitest";
import type { Account, RecurringPayment, Transaction } from "@/lib/api";
import { budgetUsage, spendingRoom } from "@/lib/budget";

const account = (balance: number, spending: boolean) =>
  ({ id: String(balance), balance, spending }) as Account;
const payment = (next_date: string, amount: number) =>
  ({ next_date, amount }) as RecurringPayment;
const salary = (date: string) =>
  ({ date, amount: 3000, category: "SALARY" }) as Transaction;

describe("spendingRoom", () => {
  const today = new Date(2026, 9, 12);

  it("leaves savings out and reserves fixed costs due before payday", () => {
    const room = spendingRoom(
      [account(1500, true), account(10000, false)],
      [payment("2026-10-15", 30), payment("2026-10-27", 500), payment("2026-11-01", 900)],
      [salary("2026-09-27")],
      today,
    );
    expect(room.payday).toBe("2026-10-27");
    expect(room.balance).toBe(1500);
    expect(room.reserved).toBe(30);
    expect(room.room).toBe(1470);
    expect(room.daysLeft).toBe(15);
    expect(room.perDay).toBe(98);
    expect(room.cycleStart).toBe("2026-09-27");
  });

  it("rolls payday forward and falls back to the end of the month", () => {
    expect(spendingRoom([], [], [salary("2026-08-27")], today).payday).toBe("2026-10-27");
    const fallback = spendingRoom([], [], [], today);
    expect(fallback.payday).toBe("2026-10-31");
    expect(fallback.basedOnSalary).toBe(false);
  });
});

describe("budgetUsage", () => {
  const booking = (date: string, amount: number, category: string, splits?: Transaction["splits"]) =>
    ({ date, amount, category, splits }) as Transaction;

  it("counts split parts and cash in their category since the cycle began", () => {
    const usage = budgetUsage(
      [{ category: "GROCERIES", limit: 100 }, { category: "DINING", limit: 50 }, { category: "HEALTH", limit: 20 }],
      [
        booking("2026-09-26", -500, "GROCERIES"),
        booking("2026-09-28", -60.1, "GROCERIES"),
        booking("2026-09-29", -30, "SHOPPING", [{ category: "GROCERIES", amount: -20 }, { category: "HEALTH", amount: -10 }]),
        booking("2026-10-01", 15, "GROCERIES"),
        booking("2026-10-02", -55, "DINING"),
        booking("2026-10-03", -40, "CASH"),
      ],
      "2026-09-27",
    );
    expect(usage.map((entry) => [entry.category, entry.spent, entry.state])).toEqual([
      ["DINING", 55, "over"],
      ["GROCERIES", 80.1, "close"],
      ["HEALTH", 10, "fine"],
    ]);
  });
});
