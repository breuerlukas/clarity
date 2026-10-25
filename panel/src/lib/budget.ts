import type { Account, CashWallet, Category, CategoryBudget, RecurringPayment, Transaction } from "@/lib/api";
import { daysBetween, isoDate } from "@/lib/dates";
import { bookingKey, total } from "@/lib/overview";
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
  /** What the last salary brought, null when none is known. */
  salary: number | null;
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
    space: wallet.space,
    hidden: false,
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
/** A payment at least this large can be a salary; smaller ones are refunds and gifts. */
const MINIMUM_SALARY = 300;

/**
 * The salary even when its booking doesn't say "Gehalt": the largest income
 * that arrives about monthly from the same payer. Its latest bookings must be
 * 25 to 36 days apart, which allows for weekends and holidays.
 */
export function regularIncome(transactions: Transaction[]): Transaction[] {
  const payers = new Map<string, Transaction[]>();
  for (const transaction of transactions.filter((entry) => isCounted(entry) && entry.amount >= MINIMUM_SALARY)) {
    payers.set(bookingKey(transaction), [...(payers.get(bookingKey(transaction)) ?? []), transaction]);
  }
  const monthly = [...payers.values()]
    .map((bookings) => bookings.sort((first, second) => first.date.localeCompare(second.date)).slice(-3))
    .filter(
      (bookings) =>
        bookings.length >= 2 &&
        bookings.slice(1).every((booking, index) => {
          const gap = daysBetween(bookings[index].date, booking.date);
          return gap >= 25 && gap <= 36;
        }),
    );
  return monthly.sort((first, second) => second.at(-1)!.amount - first.at(-1)!.amount)[0] ?? [];
}

function lastSalary(transactions: Transaction[]): Transaction | undefined {
  return [
    ...transactions.filter((transaction) => transaction.category === "SALARY" && transaction.amount > 0),
    ...regularIncome(transactions),
  ]
    .sort((first, second) => first.date.localeCompare(second.date) || first.amount - second.amount)
    .at(-1);
}

export function spendingRoom(
  accounts: Account[],
  payments: RecurringPayment[],
  transactions: Transaction[],
  today = new Date(),
  setAside = 0,
): SpendingRoom {
  const start = new Date(today.getFullYear(), today.getMonth(), today.getDate());
  const salary = lastSalary(transactions);
  const lastSalaryDate = salary?.date;
  let cycleStart = lastSalaryDate ? new Date(`${lastSalaryDate}T00:00:00`) : new Date(start.getFullYear(), start.getMonth(), 1);
  let payday = lastSalaryDate ? sameDayNextMonth(cycleStart) : new Date(start.getFullYear(), start.getMonth() + 1, 0);
  while (lastSalaryDate && payday <= start) {
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
    basedOnSalary: Boolean(lastSalaryDate),
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
    salary: salary?.amount ?? null,
  };
}

export interface MonthlyBudget {
  limit: number;
  /** Salary minus fixed costs, because the owner set no budget. */
  suggested: boolean;
  spent: number;
  left: number;
  perDay: number;
  /** Spent as a share of the limit, at most 1. */
  share: number;
}

/**
 * What is left of the money meant for this pay cycle, independent of what
 * lies on the account. Only variable spending counts: fixed costs are part
 * of the suggestion already, and an owner's own budget is meant for what
 * comes on top. Without a budget and without a known salary there is none.
 */
export function monthlyBudget(
  budget: number | null,
  room: SpendingRoom,
  payments: RecurringPayment[],
  transactions: Transaction[],
  today = new Date(),
): MonthlyBudget | null {
  const suggestion = room.salary === null ? null : total([room.salary, -total(payments.map((payment) => payment.monthly_amount))]);
  const limit = budget ?? suggestion;
  if (limit === null || limit <= 0) {
    return null;
  }
  const fixed = new Set(payments.map((payment) => payment.key));
  const todayIso = isoDate(today);
  const spent = -total(
    categoryParts(transactions)
      .filter(isCounted)
      .filter((part) => part.amount < 0 && part.date >= room.cycleStart && part.date <= todayIso && !fixed.has(bookingKey(part)))
      .map((part) => part.amount),
  );
  const left = total([limit, -spent]);
  return {
    limit,
    suggested: budget === null,
    spent,
    left,
    perDay: Math.max(0, Math.floor((left / room.daysLeft) * 100) / 100),
    share: Math.min(1, spent / limit),
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
