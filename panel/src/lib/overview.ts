import type { Category, RecurringPayment, Transaction } from "@/lib/api";
import { isoDate, monthRange, type MonthKey } from "@/lib/dates";
import { categoryParts, isCounted } from "@/lib/statistics";

export interface MonthSummary {
  income: number;
  spending: number;
  categories: { category: Category; amount: number }[];
}

const cents = (amount: number) => Math.round(amount * 100);

export function summarizeMonth(transactions: Transaction[]): MonthSummary {
  const counted = categoryParts(transactions).filter(isCounted);
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

/** How long ahead of the last day to cancel the overview reminds of it. */
const REMINDER_DAYS = 14;

/**
 * Running fixed costs whose last day to cancel before the next renewal is
 * today or within the next two weeks, soonest first.
 */
export function cancellationReminders(payments: RecurringPayment[], today: string): RecurringPayment[] {
  const until = new Date(`${today}T12:00:00`);
  until.setDate(until.getDate() + REMINDER_DAYS);
  const last = isoDate(until);
  const dueSoon = (cancelBy: string | null) => cancelBy !== null && cancelBy >= today && cancelBy <= last;
  return payments
    .filter((payment) => payment.status !== "ENDED" && !payment.cancelled_on && dueSoon(payment.cancel_by))
    .sort((first, second) => String(first.cancel_by).localeCompare(String(second.cancel_by)));
}

/**
 * The bookings a fixed cost was found in, newest first: debits grouped the
 * way the api's RecurringPaymentDetector groups them, by counterparty or,
 * without one, by purpose, ignoring case and spacing.
 */
export function recurringBookings(payment: RecurringPayment, transactions: Transaction[]): Transaction[] {
  const key = (transaction: Transaction) =>
    (transaction.counterparty.trim() ? transaction.counterparty : transaction.description)
      .trim()
      .toLowerCase()
      .replace(/\s+/g, " ");
  return transactions
    .filter((transaction) => transaction.amount < 0 && transaction.category !== "TRANSFER" && key(transaction) === payment.key)
    .sort((first, second) => second.date.localeCompare(first.date));
}

/** The owner's name for a fixed cost, else who it goes to. */
export function paymentName(payment: RecurringPayment): string {
  return payment.name || payment.counterparty;
}

export function total(amounts: number[]): number {
  return amounts.reduce((sum, amount) => sum + cents(amount), 0) / 100;
}

const PER_YEAR: Record<RecurringPayment["cycle"], number> = { WEEKLY: 52, MONTHLY: 12, QUARTERLY: 4, YEARLY: 1 };

/**
 * What a payment costs a year, from the amount itself: going through the
 * rounded monthly share would turn 289,40 € a year into 289,44 €.
 */
export function yearly(payment: RecurringPayment) {
  return payment.amount * PER_YEAR[payment.cycle];
}
