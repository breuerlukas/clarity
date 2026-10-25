import type { BusinessTransaction, Connection, Contract } from "@/lib/api";
import { INTERVAL_MONTHS } from "@/lib/business";
import { addDays, addMonths, isoDate } from "@/lib/dates";

/**
 * A small web agency on the side: a dozen customer contracts started over
 * the last two years (one ended, one cancelled), a business account where
 * the invoices come in, and a monthly private withdrawal to the demo's
 * personal current account. Dates are relative to today, so the demo always
 * has deadlines coming up.
 */
const PERSONAL_IBAN = "DE89370400440532013000";

interface DemoContract {
  customer: string;
  amount: number;
  interval: Contract["interval"];
  /** Months before today it started; negative for one that starts later. */
  startedMonthsAgo: number;
  startDay: number;
  term: number;
  notice: number;
  /** Months from today to its last day, negative for one that ended. */
  endsInMonths?: number;
  /** How the customer's bank names it. */
  payer: string;
}

const CONTRACTS: DemoContract[] = [
  { customer: "Bäckerei Hoffmann", amount: 89, interval: "MONTHLY", startedMonthsAgo: 22, startDay: 1, term: 12, notice: 3, payer: "BAECKEREI HOFFMANN E.K." },
  { customer: "Kanzlei Weber & Partner", amount: 1450, interval: "QUARTERLY", startedMonthsAgo: 17, startDay: 1, term: 24, notice: 3, payer: "Weber und Partner Rechtsanwaelte" },
  { customer: "Studio Lindgren", amount: 240, interval: "MONTHLY", startedMonthsAgo: 14, startDay: 15, term: 12, notice: 1, payer: "Studio Lindgren GmbH" },
  { customer: "Autohaus Brandt GmbH", amount: 4800, interval: "YEARLY", startedMonthsAgo: 13, startDay: 1, term: 12, notice: 3, payer: "AUTOHAUS BRANDT GMBH" },
  { customer: "Praxis Dr. Yilmaz", amount: 129, interval: "MONTHLY", startedMonthsAgo: 11, startDay: 1, term: 12, notice: 2, payer: "Praxis Dr. med. Yilmaz" },
  { customer: "Gärtnerei Sommer", amount: 59, interval: "MONTHLY", startedMonthsAgo: 10, startDay: 1, term: 6, notice: 1, endsInMonths: -3, payer: "Gaertnerei Sommer" },
  { customer: "Nordlicht Events", amount: 1200, interval: "HALF_YEARLY", startedMonthsAgo: 8, startDay: 1, term: 12, notice: 3, payer: "Nordlicht Events UG" },
  { customer: "Café Mira", amount: 49, interval: "MONTHLY", startedMonthsAgo: 7, startDay: 10, term: 12, notice: 3, endsInMonths: 1, payer: "Cafe Mira" },
  { customer: "Studio Lindgren", amount: 600, interval: "QUARTERLY", startedMonthsAgo: 5, startDay: 1, term: 12, notice: 3, payer: "Studio Lindgren GmbH" },
  { customer: "Fahrradladen Kurbel", amount: 99, interval: "MONTHLY", startedMonthsAgo: 4, startDay: 1, term: 12, notice: 3, payer: "Kurbel Fahrradladen" },
  { customer: "Immobilien Krause", amount: 350, interval: "MONTHLY", startedMonthsAgo: 2, startDay: 1, term: 24, notice: 6, payer: "Krause Immobilien GmbH & Co. KG" },
  { customer: "Hotel Seeblick", amount: 2900, interval: "YEARLY", startedMonthsAgo: -1, startDay: 1, term: 12, notice: 3, payer: "Hotel Seeblick" },
];

const EXPENSES = [
  { counterparty: "Hetzner Online GmbH", amount: 64.9, day: 3 },
  { counterparty: "Adobe Systems Software", amount: 66.45, day: 8 },
  { counterparty: "Steuerbüro Lange", amount: 178.5, day: 20 },
];

const monthStart = (today: Date, monthsAgo: number, day: number) =>
  addMonths(isoDate(new Date(today.getFullYear(), today.getMonth(), Math.min(day, 28))), -monthsAgo);

export const BUSINESS_SPACE = "agency";

export function demoContracts(today: Date): Contract[] {
  return CONTRACTS.map((contract, index) => ({
    id: `contract-${index + 1}`,
    space: BUSINESS_SPACE,
    customer: contract.customer,
    amount: contract.amount,
    interval: contract.interval,
    start_date: monthStart(today, contract.startedMonthsAgo, contract.startDay),
    term_months: contract.term,
    notice_months: contract.notice,
    end_date:
      contract.endsInMonths === undefined
        ? null
        : addDays(monthStart(today, -contract.endsInMonths, 1), -1),
    created_at: today.getTime(),
  }));
}

export function demoBusinessConnections(today: Date): Connection[] {
  return [
    {
      id: "business-bank",
      space: BUSINESS_SPACE,
      bank_name: "Qonto",
      bank_country: "DE",
      valid_until: today.getTime() + 1000 * 60 * 60 * 24 * 121,
      accounts: [
        {
          id: "business",
          space: BUSINESS_SPACE,
          hidden: false,
          name: "Geschäftskonto",
          iban: "DE44100110012620044218",
          currency: "EUR",
          balance: 18430.12,
          last_synced_at: today.getTime() - 1000 * 60 * 47,
          sync_failure: null,
          spending: false,
        },
      ],
    },
  ];
}

/** Invoices paid a few days after they are due, expenses and withdrawals. */
export function demoBusinessTransactions(today: Date, contracts: Contract[]): BusinessTransaction[] {
  const todayIso = isoDate(today);
  const from = addMonths(todayIso, -6);
  const transactions: BusinessTransaction[] = [];
  const add = (date: string, amount: number, counterparty: string, description: string, iban: string | null = null) => {
    if (date < from || date > todayIso) {
      return;
    }
    transactions.push({
      id: `business-${transactions.length + 1}`,
      account: "business",
      date,
      amount,
      currency: "EUR",
      counterparty,
      counterparty_iban: iban,
      description,
      category: amount > 0 ? "INCOME" : "OTHER",
      assignment: null,
      invoices: [],
    });
  };
  contracts.forEach((contract, index) => {
    const payer = CONTRACTS[index].payer;
    const step = INTERVAL_MONTHS[contract.interval];
    for (let due = contract.start_date; due <= todayIso; due = addMonths(due, step)) {
      if (contract.end_date && due > contract.end_date) {
        break;
      }
      add(addDays(due, 3 + (index % 4)), contract.amount, payer, `Rechnung ${due.slice(0, 7)}`);
    }
  });
  for (let month = 0; month <= 6; month += 1) {
    const first = monthStart(today, month, 1);
    EXPENSES.forEach((expense) => add(addDays(first, expense.day - 1), -expense.amount, expense.counterparty, "Lastschrift"));
    add(addDays(first, 24), -2500, "Demo", "Privatentnahme", PERSONAL_IBAN);
  }
  add(addDays(todayIso, -40), 3000, "Demo", "Einlage Laptop", PERSONAL_IBAN);
  add(addDays(todayIso, -12), 380, "Messe Nord GmbH", "Workshop einmalig");
  return transactions.sort((first, second) => second.date.localeCompare(first.date));
}
