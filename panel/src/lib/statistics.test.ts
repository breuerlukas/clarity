import { describe, expect, it } from "vitest";
import type { Transaction } from "@/lib/api";
import {
  balanceHistory,
  categoryMatrix,
  lastMonths,
  monthlyStatistics,
  topCounterparties,
} from "@/lib/statistics";

const transaction = (
  date: string,
  amount: number,
  category: Transaction["category"] = "OTHER",
  counterparty = "x",
): Transaction => ({
  id: crypto.randomUUID(),
  account: "a",
  date,
  amount,
  currency: "EUR",
  counterparty,
  description: "",
  category,
});

describe("monthlyStatistics", () => {
  it("sums each month, ignores transfers and computes the savings rate", () => {
    const statistics = monthlyStatistics(
      [
        transaction("2026-09-28", 2000, "SALARY"),
        transaction("2026-09-03", -500),
        transaction("2026-09-10", -300, "TRANSFER"),
        transaction("2026-10-01", -100),
      ],
      ["2026-09", "2026-10"],
    );
    expect(statistics[0]).toEqual({
      month: "2026-09",
      income: 2000,
      spending: 500,
      net: 1500,
      savingsRate: 0.75,
    });
    expect(statistics[1].savingsRate).toBeNull();
  });
});

describe("balanceHistory", () => {
  it("walks back from today's balance one day at a time", () => {
    const history = balanceHistory(
      [transaction("2026-10-10", -20), transaction("2026-10-12", 100)],
      1000,
      "2026-10-12",
    );
    expect(history).toEqual([
      { date: "2026-10-10", balance: 900 },
      { date: "2026-10-11", balance: 900 },
      { date: "2026-10-12", balance: 1000 },
    ]);
  });
});

describe("categoryMatrix and topCounterparties", () => {
  it("groups spending by category per month and by counterparty", () => {
    const transactions = [
      transaction("2026-09-02", -10, "GROCERIES", "REWE"),
      transaction("2026-10-02", -30, "GROCERIES", "REWE"),
      transaction("2026-10-03", -50, "DINING", "Vapiano"),
    ];
    const rows = categoryMatrix(transactions, ["2026-09", "2026-10"]);
    expect(rows[0]).toEqual({
      category: "DINING",
      amounts: [0, 50],
      total: 50,
      average: 25,
    });
    expect(rows[1].amounts).toEqual([10, 30]);
    expect(topCounterparties(transactions, 1)).toEqual([
      { name: "Vapiano", amount: 50, count: 1 },
    ]);
  });

  it("lists months oldest first", () => {
    expect(lastMonths(3, new Date(2026, 0, 15))).toEqual([
      "2025-11",
      "2025-12",
      "2026-01",
    ]);
  });
});

describe("categoryParts", () => {
  it("counts the parts of a split transaction and leaves ATM withdrawals out", () => {
    const rows = categoryMatrix(
      [
        {
          ...transaction("2026-10-02", -10, "GROCERIES"),
          splits: [
            { category: "GROCERIES", amount: -7 },
            { category: "HEALTH", amount: -3 },
          ],
        },
        transaction("2026-10-03", -100, "CASH"),
      ],
      ["2026-10"],
    );
    expect(rows.map((row) => [row.category, row.total])).toEqual([
      ["GROCERIES", 7],
      ["HEALTH", 3],
    ]);
  });
});