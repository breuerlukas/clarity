import type {
  BillingInterval,
  BusinessTransaction,
  Contract,
  PaymentKind,
} from "@/lib/api";
import {
  addDays,
  addMonths,
  daysBetween,
  isoDate,
  monthRange,
  type MonthKey,
} from "@/lib/dates";
import { total } from "@/lib/overview";

export const INTERVAL_MONTHS: Record<BillingInterval, number> = {
  MONTHLY: 1,
  QUARTERLY: 3,
  HALF_YEARLY: 6,
  YEARLY: 12,
};

export type ContractStatus = "planned" | "active" | "cancelled" | "ended";

/** What a contract brings in per month, whatever its billing interval. */
export function monthlyValue(contract: Contract): number {
  return contract.amount / INTERVAL_MONTHS[contract.interval];
}

/** Running on that day: started, and the end date (its last day) not passed. */
export function isActiveOn(contract: Contract, date: string): boolean {
  return contract.start_date <= date && (!contract.end_date || date <= contract.end_date);
}

/**
 * Planned before the start, cancelled once an end date is set that has not
 * come yet, ended after it.
 */
export function contractStatus(contract: Contract, today: string): ContractStatus {
  if (contract.start_date > today) {
    return "planned";
  }
  if (contract.end_date && contract.end_date < today) {
    return "ended";
  }
  return contract.end_date ? "cancelled" : "active";
}

/** The last day of the contract's `term`-th term (counting from one). */
function termEnd(contract: Contract, term: number): string {
  return addDays(addMonths(contract.start_date, term * contract.term_months), -1);
}

/**
 * The last day a cancellation for the `term`-th term must arrive:
 * `notice_months` before the term ends ("drei Monate zum Laufzeitende").
 */
function noticeDeadline(contract: Contract, term: number): string {
  return addDays(
    addMonths(contract.start_date, term * contract.term_months - contract.notice_months),
    -1,
  );
}

/**
 * The earliest way out: the next notice deadline that has not passed, and
 * the day the contract would then end. A missed deadline means the contract
 * already renewed, so it moves to the term after.
 */
export function nextExit(contract: Contract, today: string): { deadline: string; end: string } {
  let term = 1;
  while (noticeDeadline(contract, term) < today) {
    term += 1;
  }
  return { deadline: noticeDeadline(contract, term), end: termEnd(contract, term) };
}

/** The day the running term renews on its own, the first day of the next. */
export function nextRenewal(contract: Contract, today: string): string {
  let term = 1;
  while (termEnd(contract, term) < today) {
    term += 1;
  }
  return addDays(termEnd(contract, term), 1);
}

export type ContractEventKind = "deadline" | "renewal" | "end" | "start";

export interface ContractEvent {
  contract: Contract;
  kind: ContractEventKind;
  date: string;
  days: number;
}

/**
 * What is coming up within `horizon` days, soonest first: for running
 * contracts the last day to cancel and the renewal, for cancelled ones the
 * end, for planned ones the start.
 */
export function upcomingEvents(
  contracts: Contract[],
  today: string,
  horizon = 90,
): ContractEvent[] {
  const events: ContractEvent[] = [];
  const add = (contract: Contract, kind: ContractEventKind, date: string) => {
    const days = daysBetween(today, date);
    if (days >= 0 && days <= horizon) {
      events.push({ contract, kind, date, days });
    }
  };
  for (const contract of contracts) {
    const status = contractStatus(contract, today);
    if (status === "planned") {
      add(contract, "start", contract.start_date);
    } else if (status === "cancelled") {
      add(contract, "end", contract.end_date!);
    } else if (status === "active") {
      add(contract, "deadline", nextExit(contract, today).deadline);
      add(contract, "renewal", nextRenewal(contract, today));
    }
  }
  return events.sort(
    (first, second) => first.date.localeCompare(second.date) || first.kind.localeCompare(second.kind),
  );
}

/** "Müller GmbH" and "MUELLER GMBH " are the same customer. */
export function customerKey(name: string): string {
  return normalizeName(name);
}

export interface CustomerContracts {
  customer: string;
  contracts: Contract[];
  /** Monthly revenue from this customer today. */
  monthly: number;
}

/** Contracts per customer, the biggest customer first, ended ones last. */
export function contractsByCustomer(contracts: Contract[], today: string): CustomerContracts[] {
  const groups = new Map<string, Contract[]>();
  for (const contract of contracts) {
    const key = customerKey(contract.customer);
    groups.set(key, [...(groups.get(key) ?? []), contract]);
  }
  return [...groups.values()]
    .map((group) => ({
      customer: group[group.length - 1].customer,
      contracts: [...group].sort((first, second) => second.start_date.localeCompare(first.start_date)),
      monthly: total(group.filter((contract) => isActiveOn(contract, today)).map(monthlyValue)),
    }))
    .sort(
      (first, second) =>
        second.monthly - first.monthly || first.customer.localeCompare(second.customer),
    );
}

export interface RevenuePoint {
  month: MonthKey;
  /** Monthly recurring revenue at the end of the month (today for this one). */
  mrr: number;
  arr: number;
  customers: number;
  /**
   * Share of the customers at the end of the month before who had no running
   * contract anymore at the end of this one; null without customers before.
   */
  churn: number | null;
}

/**
 * MRR, ARR, customers and churn per month, all derived from the contracts:
 * each month is measured on its last day, the current one today.
 */
export function revenueHistory(
  contracts: Contract[],
  months: MonthKey[],
  today = new Date(),
): RevenuePoint[] {
  const todayIso = isoDate(today);
  const measureDay = (month: MonthKey) => {
    const last = monthRange(month).to;
    return last < todayIso ? last : todayIso;
  };
  const customersOn = (date: string) =>
    new Set(contracts.filter((contract) => isActiveOn(contract, date)).map((contract) => customerKey(contract.customer)));
  return months.map((month) => {
    const date = measureDay(month);
    const mrr = total(contracts.filter((contract) => isActiveOn(contract, date)).map(monthlyValue));
    const customers = customersOn(date);
    const before = customersOn(addDays(monthRange(month).from, -1));
    const lost = [...before].filter((customer) => !customers.has(customer)).length;
    return {
      month,
      mrr,
      arr: total([mrr * 12]),
      customers: customers.size,
      churn: before.size > 0 ? lost / before.size : null,
    };
  });
}

export interface PaymentMatch {
  kind: PaymentKind;
  contract?: Contract;
  /** False when the owner assigned it by hand. */
  automatic: boolean;
}

const LEGAL_FORMS = new Set([
  "gmbh", "mbh", "ag", "ug", "kg", "ohg", "gbr", "ek", "ev", "eg", "se", "co",
  "haftungsbeschraenkt", "ltd", "llc", "inc", "bv", "sarl", "sl", "sa", "kgaa",
]);

/** Lower case, umlauts spelled out, no punctuation and no legal form. */
function normalizeName(name: string): string {
  return name
    .toLowerCase()
    .replace(/ä/g, "ae")
    .replace(/ö/g, "oe")
    .replace(/ü/g, "ue")
    .replace(/ß/g, "ss")
    .normalize("NFKD")
    .replace(/[̀-ͯ]/g, "")
    .replace(/[^a-z0-9]+/g, " ")
    .split(" ")
    .filter((word) => word && !LEGAL_FORMS.has(word))
    .join(" ");
}

const compactIban = (iban: string | null | undefined) =>
  (iban ?? "").replace(/\s/g, "").toUpperCase();

/**
 * The months around a contract in which its payments can arrive: invoices
 * paid in advance, or the last one paid a little late.
 */
function expectsPaymentOn(contract: Contract, date: string): boolean {
  return (
    addMonths(contract.start_date, -1) <= date &&
    (!contract.end_date || date <= addMonths(contract.end_date, 2))
  );
}

/**
 * What a business booking is. The owner's own choice wins. Otherwise money
 * to or from one of the owner's personal accounts is a private withdrawal or
 * deposit, and money in whose payer or reference names a customer pays that
 * customer's contract; with several, the one with the same amount.
 */
export function matchPayment(
  transaction: BusinessTransaction,
  contracts: Contract[],
  personalIbans: Set<string>,
): PaymentMatch {
  if (transaction.assignment) {
    const contract = contracts.find((entry) => entry.id === transaction.assignment!.contract);
    if (transaction.assignment.kind !== "CONTRACT" || contract) {
      return { kind: transaction.assignment.kind, contract, automatic: false };
    }
  }
  const iban = compactIban(transaction.counterparty_iban);
  if (iban && personalIbans.has(iban)) {
    return { kind: transaction.amount < 0 ? "WITHDRAWAL" : "DEPOSIT", automatic: true };
  }
  if (transaction.amount <= 0) {
    return { kind: "NONE", automatic: true };
  }
  const text = ` ${normalizeName(`${transaction.counterparty} ${transaction.description}`)} `;
  const candidates = contracts.filter((contract) => {
    const name = normalizeName(contract.customer);
    return name && expectsPaymentOn(contract, transaction.date) && text.includes(` ${name} `);
  });
  const contract =
    candidates.find((candidate) => Math.abs(candidate.amount - transaction.amount) < 0.005) ??
    candidates[0];
  return contract ? { kind: "CONTRACT", contract, automatic: true } : { kind: "NONE", automatic: true };
}

export function personalIbans(ibans: (string | null)[]): Set<string> {
  return new Set(ibans.map(compactIban).filter(Boolean));
}
