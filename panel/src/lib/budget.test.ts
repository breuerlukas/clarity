import { describe, expect, it } from "vitest";
import type { Account, RecurringPayment, Transaction } from "@/lib/api";
import { budgetUsage, monthlyBudget, regularIncome, spendingRoom } from "@/lib/budget";

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

  it("takes money set aside for goals out of the room", () => {
    const room = spendingRoom([account(1500, true)], [], [salary("2026-09-27")], today, 400);
    expect(room.setAside).toBe(400);
    expect(room.room).toBe(1100);
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

describe("regularIncome", () => {
  const income = (date: string, amount: number, counterparty: string, category = "INCOME") =>
    ({ date, amount, counterparty, description: "", category }) as Transaction;

  it("takes the largest monthly income as salary, whatever its category", () => {
    const bookings = regularIncome([
      income("2026-08-29", 2480, "Brightline GmbH"),
      income("2026-09-30", 2480, "Brightline GmbH"),
      income("2026-10-01", 450, "Minijob KG"),
      income("2026-09-01", 450, "Minijob KG"),
      income("2026-10-05", 5000, "Erbschaft"),
      income("2026-10-02", 3000, "Brightline GmbH", "TRANSFER"),
    ]);
    expect(bookings.map((booking) => booking.date)).toEqual(["2026-08-29", "2026-09-30"]);
  });

  it("starts the pay cycle on it without the word Gehalt", () => {
    const room = spendingRoom([], [], [income("2026-08-28", 2480, "Brightline GmbH"), income("2026-09-28", 2480, "Brightline GmbH")], new Date(2026, 9, 12));
    expect(room.basedOnSalary).toBe(true);
    expect(room.cycleStart).toBe("2026-09-28");
    expect(room.salary).toBe(2480);
  });
});

describe("monthlyBudget", () => {
  const today = new Date(2026, 9, 12);
  const room = spendingRoom([], [], [salary("2026-09-27")], today);
  const rent = { key: "hausverwaltung", monthly_amount: 1050 } as RecurringPayment;
  const booking = (date: string, amount: number, counterparty: string, category = "GROCERIES") =>
    ({ date, amount, counterparty, description: "", category }) as Transaction;
  const bookings = [
    booking("2026-09-26", -80, "REWE"),
    booking("2026-10-01", -1050, "Hausverwaltung"),
    booking("2026-10-02", -120, "REWE"),
    booking("2026-10-03", -40, "Geldautomat", "CASH"),
    booking("2026-10-04", -30, "Lieferando"),
  ];

  it("suggests salary minus fixed costs and counts only variable spending since payday", () => {
    const budget = monthlyBudget(null, room, [rent], bookings, today)!;
    expect(budget).toMatchObject({ limit: 1950, suggested: true, spent: 150, left: 1800, perDay: 120 });
  });

  it("uses the owner's own budget, and none without salary or budget", () => {
    expect(monthlyBudget(600, room, [rent], bookings, today)).toMatchObject({ limit: 600, suggested: false, left: 450, share: 0.25 });
    expect(monthlyBudget(null, spendingRoom([], [], [], today), [], bookings, today)).toBeNull();
  });
});
