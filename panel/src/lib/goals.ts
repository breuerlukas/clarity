import type { Account, SavingsGoal } from "@/lib/api";
import { total } from "@/lib/overview";

export interface GoalProgress {
  saved: number;
  /** Saved as a share of the target, at most 1. */
  share: number;
  left: number;
  /** What to put aside each month to make the target date; null without one. */
  perMonth: number | null;
}

function savedFor(goal: SavingsGoal, accounts: Account[]): number {
  if (!goal.account) {
    return goal.saved;
  }
  return Math.max(0, accounts.find((account) => account.id === goal.account)?.balance ?? 0);
}

/** Whole months from today until the target date, at least one. */
function monthsUntil(today: string, date: string): number {
  const [fromYear, fromMonth, fromDay] = today.split("-").map(Number);
  const [toYear, toMonth, toDay] = date.split("-").map(Number);
  const months = (toYear - fromYear) * 12 + toMonth - fromMonth - (toDay < fromDay ? 1 : 0);
  return Math.max(1, months);
}

/**
 * How far a goal is, from what was set aside by hand or the balance of its
 * account. A target date already passed asks for the rest at once.
 */
export function goalProgress(goal: SavingsGoal, accounts: Account[], today: string): GoalProgress {
  const saved = savedFor(goal, accounts);
  const left = Math.max(0, total([goal.target, -saved]));
  const perMonth = goal.target_date === null ? null : Math.ceil((left / monthsUntil(today, goal.target_date)) * 100) / 100;
  return { saved, share: Math.min(1, saved / goal.target), left, perMonth };
}

/**
 * The money meant for goals that still lies on the everyday accounts and so
 * isn't free until payday: everything set aside by hand, and the balance of
 * a linked everyday account up to the target. A linked savings account is
 * already left out of the spending room.
 */
export function setAsideForGoals(goals: SavingsGoal[], accounts: Account[]): number {
  return total(
    goals.map((goal) => {
      if (!goal.account) {
        return goal.saved;
      }
      const account = accounts.find((entry) => entry.id === goal.account);
      return account?.spending ? Math.min(goal.target, Math.max(0, account.balance ?? 0)) : 0;
    }),
  );
}
