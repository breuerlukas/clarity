import type { Category, Transaction } from "@/lib/api";
import { currentMonth, isoDate, shiftMonth, type MonthKey } from "@/lib/dates";

const cents = (amount: number) => Math.round(amount * 100);
const isCounted = (transaction: Transaction) =>
  transaction.category !== "TRANSFER";
const monthOf = (date: string): MonthKey => date.slice(0, 7);

export interface MonthStatistic {
  month: MonthKey;
  income: number;
  spending: number;
  net: number;
  /** Share of the income that was kept, null without income. */
  savingsRate: number | null;
}

/** The last `count` months, oldest first, ending with the current one. */
export function lastMonths(count: number, today = new Date()): MonthKey[] {
  const now = currentMonth(today);
  return Array.from({ length: count }, (_, index) =>
    shiftMonth(now, index - count + 1),
  );
}

export function monthlyStatistics(
  transactions: Transaction[],
  months: MonthKey[],
): MonthStatistic[] {
  const totals = new Map(months.map((month) => [month, { in: 0, out: 0 }]));
  for (const transaction of transactions.filter(isCounted)) {
    const total = totals.get(monthOf(transaction.date));
    if (!total) {
      continue;
    }
    if (transaction.amount > 0) {
      total.in += cents(transaction.amount);
    } else {
      total.out -= cents(transaction.amount);
    }
  }
  return months.map((month) => {
    const total = totals.get(month)!;
    return {
      month,
      income: total.in / 100,
      spending: total.out / 100,
      net: (total.in - total.out) / 100,
      savingsRate: total.in > 0 ? (total.in - total.out) / total.in : null,
    };
  });
}

/**
 * Averages over complete months only: the running month would drag every
 * figure down until the salary arrives.
 */
export function monthlyAverages(statistics: MonthStatistic[]) {
  const complete = statistics.filter(
    (statistic) => statistic.month < currentMonth(),
  );
  const average = (values: number[]) =>
    values.length === 0
      ? null
      : values.reduce((sum, value) => sum + value, 0) / values.length;
  const rates = complete
    .map((statistic) => statistic.savingsRate)
    .filter((rate): rate is number => rate !== null);
  return {
    months: complete.length,
    income: average(complete.map((statistic) => statistic.income)),
    spending: average(complete.map((statistic) => statistic.spending)),
    savingsRate: average(rates),
  };
}

/**
 * Daily closing balances, reconstructed backwards from today's balance:
 * the balance at the end of a day is the next day's balance minus that
 * next day's transactions. Starts at the oldest stored transaction, since
 * nothing before it is known.
 */
export function balanceHistory(
  transactions: Transaction[],
  currentBalance: number,
  today = isoDate(new Date()),
): { date: string; balance: number }[] {
  if (transactions.length === 0) {
    return [];
  }
  const byDay = new Map<string, number>();
  for (const transaction of transactions) {
    byDay.set(
      transaction.date,
      (byDay.get(transaction.date) ?? 0) + cents(transaction.amount),
    );
  }
  const oldest = transactions.reduce(
    (min, transaction) => (transaction.date < min ? transaction.date : min),
    today,
  );
  const history: { date: string; balance: number }[] = [];
  let balance = cents(currentBalance);
  const day = new Date(`${today}T12:00:00`);
  while (isoDate(day) >= oldest) {
    const date = isoDate(day);
    history.push({ date, balance: balance / 100 });
    balance -= byDay.get(date) ?? 0;
    day.setDate(day.getDate() - 1);
  }
  return history.reverse();
}

export interface CategoryRow {
  category: Category;
  amounts: number[];
  total: number;
  average: number;
}

/** Spending per category and month, largest categories first. */
export function categoryMatrix(
  transactions: Transaction[],
  months: MonthKey[],
): CategoryRow[] {
  const index = new Map(months.map((month, position) => [month, position]));
  const rows = new Map<Category, number[]>();
  for (const transaction of transactions.filter(isCounted)) {
    const position = index.get(monthOf(transaction.date));
    if (position === undefined || transaction.amount >= 0) {
      continue;
    }
    const amounts = rows.get(transaction.category) ?? months.map(() => 0);
    amounts[position] -= cents(transaction.amount);
    rows.set(transaction.category, amounts);
  }
  return [...rows.entries()]
    .map(([category, amounts]) => {
      const total = amounts.reduce((sum, amount) => sum + amount, 0);
      return {
        category,
        amounts: amounts.map((amount) => amount / 100),
        total: total / 100,
        average: total / months.length / 100,
      };
    })
    .sort((first, second) => second.total - first.total);
}

/** Where most of the money went, by counterparty. */
export function topCounterparties(transactions: Transaction[], limit: number) {
  const totals = new Map<string, { amount: number; count: number }>();
  for (const transaction of transactions.filter(isCounted)) {
    if (transaction.amount >= 0 || !transaction.counterparty) {
      continue;
    }
    const total = totals.get(transaction.counterparty) ?? { amount: 0, count: 0 };
    total.amount -= cents(transaction.amount);
    total.count += 1;
    totals.set(transaction.counterparty, total);
  }
  return [...totals.entries()]
    .map(([name, total]) => ({
      name,
      amount: total.amount / 100,
      count: total.count,
    }))
    .sort((first, second) => second.amount - first.amount)
    .slice(0, limit);
}

export function largestExpenses(
  transactions: Transaction[],
  limit: number,
): Transaction[] {
  return transactions
    .filter((transaction) => isCounted(transaction) && transaction.amount < 0)
    .sort((first, second) => first.amount - second.amount)
    .slice(0, limit);
}