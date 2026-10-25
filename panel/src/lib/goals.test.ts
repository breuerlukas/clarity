import { describe, expect, it } from "vitest";
import type { Account, SavingsGoal } from "@/lib/api";
import { goalProgress, setAsideForGoals } from "@/lib/goals";

const goal = (overrides: Partial<SavingsGoal>) =>
  ({ target: 1200, target_date: null, account: null, saved: 0, ...overrides }) as SavingsGoal;
const account = (id: string, balance: number, spending: boolean) => ({ id, balance, spending }) as Account;

describe("goalProgress", () => {
  it("spreads what is left over the months until the target date", () => {
    const progress = goalProgress(goal({ saved: 300, target_date: "2027-04-12" }), [], "2026-10-12");
    expect(progress).toEqual({ saved: 300, share: 0.25, left: 900, perMonth: 150 });
  });

  it("counts a partial last month out and asks for the rest once the date passed", () => {
    expect(goalProgress(goal({ target_date: "2027-04-11" }), [], "2026-10-12").perMonth).toBe(240);
    expect(goalProgress(goal({ target_date: "2026-09-01" }), [], "2026-10-12").perMonth).toBe(1200);
  });

  it("uses the balance of a linked account instead of the manual amount", () => {
    const progress = goalProgress(goal({ account: "tagesgeld", saved: 50 }), [account("tagesgeld", 1500, false)], "2026-10-12");
    expect(progress.saved).toBe(1500);
    expect(progress.share).toBe(1);
    expect(progress.left).toBe(0);
    expect(progress.perMonth).toBeNull();
  });
});

describe("setAsideForGoals", () => {
  it("reserves manual amounts and linked everyday accounts, not savings accounts", () => {
    const accounts = [account("giro", 2000, true), account("tagesgeld", 5000, false)];
    const goals = [
      goal({ saved: 250 }),
      goal({ account: "giro", target: 800 }),
      goal({ account: "tagesgeld" }),
    ];
    expect(setAsideForGoals(goals, accounts)).toBe(1050);
  });
});
