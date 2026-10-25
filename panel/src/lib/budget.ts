import type { Account, CashWallet, Category, CategoryBudget, RecurringPayment, Transaction } from "@/lib/api";
import { isoDate } from "@/lib/dates";
import { total } from "@/lib/overview";
import { categoryParts, isCounted } from "@/lib/statistics";

export interface SpendingRoom {
  /** The next payday, or the end of the month when no salary is known. */
  payday: string;
  basedOnSalary: boolean;
  /** What is on the everyday accounts right now. */
  balance: number;
  /** Fixed costs still due before payday. */
  upcoming: RecurringPayment[];
  reserved: number;
  /** Money on the everyday accounts meant for savings goals. */
  setAside: number;
  /** What is left to spend until payday. */
  room: number;
  daysLeft: number;
  perDay: number;
  /** How far the current pay cycle has run, 0 to 1. */
  progress: number;
  /** Length of the current pay cycle in days. */
  cycleDays: number;
  /** The day the current pay cycle began. */
  cycleStart: string;
}

const DAY = 24 * 60 * 60 * 1000;

function sameDayNextMonth(date: Date) {
  const day = date.getDate();
  const next = new Date(date.getFullYear(), date.getMonth() + 1, 1);
  const last = new Date(next.getFullYear(), next.getMonth() + 1, 0).getDate();
  next.setDate(Math.min(day, last));
  return next;
}

/** The cash wallet as an everyday account: cash is money to spend. */
export function cashAccount(wallet: CashWallet, name: string): Account {
  return {
    id: wallet.id,
    name,
    iban: null,
    currency: wallet.currency,
    balance: wallet.balance,
    last_synced_at: 0,
    sync_failure: null,
    spending: true,
  };
}

/**
 * The money that is really free until the next salary: the balance of the
 * everyday accounts minus every fixed cost due before payday and what is
 * set aside for savings goals. Payday is one
 * month after the last salary booking, rolled forward until it lies ahead.
 */
export function spendingRoom(
  accounts: Account[],
  payments: RecurringPayment[],
  transactions: Transaction[],
  today = new Date(),
  setAside = 0,
): SpendingRoom {
  const start = new Date(today.getFullYear(), today.getMonth(), today.getDate());
  const lastSalary = transactions
    .filter((transaction) => transaction.category === "SALARY" && transaction.amount > 0)
    .map((transaction) => transaction.date)
    .sort()
    .at(-1);
  let cycleStart = lastSalary ? new Date(`${lastSalary}T00:00:00`) : new Date(start.getFullYear(), start.getMonth(), 1);
  let payday = lastSalary ? sameDayNextMonth(cycleStart) : new Date(start.getFullYear(), start.getMonth() + 1, 0);
  while (lastSalary && payday <= start) {
    cycleStart = payday;
    payday = sameDayNextMonth(payday);
  }
  const paydayIso = isoDate(payday);
  const todayIso = isoDate(start);
  const balance = total(
    accounts.filter((account) => account.spending).map((account) => account.balance ?? 0),
  );
  const upcoming = payments
    .filter((payment) => payment.next_date >= todayIso && payment.next_date < paydayIso)
    .sort((first, second) => first.next_date.localeCompare(second.next_date));
  const reserved = total(upcoming.map((payment) => payment.amount));
  const daysLeft = Math.max(1, Math.round((payday.getTime() - start.getTime()) / DAY));
  const room = total([balance, -reserved, -setAside]);
  const cycleDays = Math.max(1, Math.round((payday.getTime() - cycleStart.getTime()) / DAY));
  return {
    payday: paydayIso,
    basedOnSalary: Boolean(lastSalary),
    balance,
    upcoming,
    reserved,
    setAside,
    room,
    daysLeft,
    perDay: Math.max(0, Math.floor((room / daysLeft) * 100) / 100),
    progress: Math.min(1, Math.max(0, 1 - daysLeft / cycleDays)),
    cycleDays,
    cycleStart: isoDate(cycleStart),
  };
}

export interface BudgetUsage {
  category: Category;
  limit: number;
  spent: number;
  /** Spent as a share of the limit; above 1 when it ran over. */
  share: number;
  state: "fine" | "close" | "over";
}

/** From this share of a limit on, a budget shows a warning. */
const CLOSE_TO_LIMIT = 0.8;

/**
 * What went out of each budgeted category since the pay cycle began, the
 * same period the spending room uses. Split parts and cash entries count in
 * the category they are assigned to; refunds don't lower the figure, as in
 * the statistics. Fullest budgets first.
 */
export function budgetUsage(
  budgets: CategoryBudget[],
  transactions: Transaction[],
  cycleStart: string,
): BudgetUsage[] {
  const spent = new Map<Category, number>();
  for (const part of categoryParts(transactions).filter(isCounted)) {
    if (part.amount < 0 && part.date >= cycleStart) {
      spent.set(part.category, (spent.get(part.category) ?? 0) - Math.round(part.amount * 100));
    }
  }
  return budgets
    .map((budget) => {
      const amount = (spent.get(budget.category) ?? 0) / 100;
      const share = amount / budget.limit;
      const state = share > 1 ? "over" : share >= CLOSE_TO_LIMIT ? "close" : "fine";
      return { category: budget.category, limit: budget.limit, spent: amount, share, state } as const;
    })
    .sort((first, second) => second.share - first.share);
}
