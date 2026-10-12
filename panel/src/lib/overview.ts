import type { Category, RecurringPayment, Transaction } from "@/lib/api";
import { monthRange, type MonthKey } from "@/lib/dates";

export interface MonthSummary {
  income: number;
  spending: number;
  categories: { category: Category; amount: number }[];
}

/**
 * Money moved between the owner's own accounts is neither earned nor spent,
 * so transfers stay out of every total.
 */
const isCounted = (transaction: Transaction) =>
  transaction.category !== "TRANSFER";

const cents = (amount: number) => Math.round(amount * 100);

export function summarizeMonth(transactions: Transaction[]): MonthSummary {
  const counted = transactions.filter(isCounted);
  const incomeCents = counted
    .filter((transaction) => transaction.amount > 0)
    .reduce((sum, transaction) => sum + cents(transaction.amount), 0);
  const spendingByCategory = new Map<Category, number>();
  for (const transaction of counted) {
    if (transaction.amount < 0) {
      spendingByCategory.set(
        transaction.category,
        (spendingByCategory.get(transaction.category) ?? 0) -
          cents(transaction.amount),
      );
    }
  }
  const categories = [...spendingByCategory.entries()]
    .map(([category, amount]) => ({ category, amount: amount / 100 }))
    .sort((first, second) => second.amount - first.amount);
  const spendingCents = [...spendingByCategory.values()].reduce(
    (sum, amount) => sum + amount,
    0,
  );
  return { income: incomeCents / 100, spending: spendingCents / 100, categories };
}

/**
 * Fixed costs still to be booked between today and the end of the month.
 */
export function upcomingThisMonth(
  payments: RecurringPayment[],
  today: string,
  month: MonthKey,
): RecurringPayment[] {
  const { to } = monthRange(month);
  return payments.filter(
    (payment) => payment.next_date >= today && payment.next_date <= to,
  );
}

export function total(amounts: number[]): number {
  return amounts.reduce((sum, amount) => sum + cents(amount), 0) / 100;
}