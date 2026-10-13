import type {
  Account,
  BusinessTransaction,
  Category,
  Connection,
  Contract,
  RecurringPayment,
  Transaction,
} from "@/lib/api";
import { isoDate } from "@/lib/dates";
import {
  demoBusinessConnections,
  demoBusinessTransactions,
  demoContracts,
} from "@/demo/demo-business";

/**
 * Made-up but believable finances for the demo: a salaried person in a
 * German city with a current account and a savings account at one bank and
 * PayPal. Seeded, so every visitor sees the same history.
 */
const HISTORY_MONTHS = 13;

function random(seed: number) {
  let state = seed;
  return () => {
    state = (state + 0x6d2b79f5) | 0;
    let mixed = Math.imul(state ^ (state >>> 15), 1 | state);
    mixed = (mixed + Math.imul(mixed ^ (mixed >>> 7), 61 | mixed)) ^ mixed;
    return ((mixed ^ (mixed >>> 14)) >>> 0) / 4294967296;
  };
}

interface FixedCost {
  counterparty: string;
  category: Category;
  amount: number;
  day: number;
  /** Months it is booked in; all when missing. */
  months?: number[];
  /** The price since the current month, to show a price increase. */
  raisedTo?: number;
}

const FIXED_COSTS: FixedCost[] = [
  { counterparty: "Hausverwaltung Schmidt GmbH", category: "HOUSING", amount: 1050, day: 1 },
  { counterparty: "Trade Republic", category: "SAVINGS", amount: 250, day: 2 },
  { counterparty: "Netflix International B.V.", category: "SUBSCRIPTIONS", amount: 13.99, day: 3, raisedTo: 15.99 },
  { counterparty: "Spotify AB", category: "SUBSCRIPTIONS", amount: 10.99, day: 5 },
  { counterparty: "Telekom Deutschland GmbH", category: "UTILITIES", amount: 39.95, day: 7 },
  { counterparty: "Stadtwerke München", category: "UTILITIES", amount: 74, day: 10 },
  { counterparty: "McFit GmbH", category: "LEISURE", amount: 29.9, day: 15 },
  { counterparty: "HUK-Coburg", category: "INSURANCE", amount: 289.4, day: 12, months: [1] },
];

const VARIABLE = [
  { category: "GROCERIES" as const, names: ["REWE Markt GmbH", "Lidl Dienstleistung", "EDEKA Center", "dm-drogerie markt", "Alnatura"], perMonth: [9, 13], range: [6, 82] },
  { category: "DINING" as const, names: ["Lieferando.de", "Vapiano", "Starbucks Coffee", "Wirtshaus am Markt"], perMonth: [3, 7], range: [7, 58] },
  { category: "MOBILITY" as const, names: ["DB Vertrieb GmbH", "MVG Automat", "Aral Station"], perMonth: [1, 4], range: [3, 64] },
  { category: "HEALTH" as const, names: ["Apotheke am Markt", "Fielmann AG"], perMonth: [0, 2], range: [5, 42] },
  { category: "LEISURE" as const, names: ["Kino Mathäser", "Eventim", "Steam Purchase"], perMonth: [0, 2], range: [9, 68] },
];

const PAYPAL_SHOPS = ["Zalando SE", "AMAZON EU S.A R.L.", "IKEA Deutschland", "Thalia", "About You"];

const TODAY = () => new Date();

function* months(today: Date) {
  for (let offset = HISTORY_MONTHS - 1; offset >= 0; offset -= 1) {
    yield new Date(today.getFullYear(), today.getMonth() - offset, 1);
  }
}

function day(month: Date, dayOfMonth: number) {
  const last = new Date(month.getFullYear(), month.getMonth() + 1, 0).getDate();
  return new Date(month.getFullYear(), month.getMonth(), Math.min(dayOfMonth, last));
}

export interface DemoDatabase {
  connections: Connection[];
  transactions: Transaction[];
  recurring: RecurringPayment[];
  businessConnections: Connection[];
  businessTransactions: BusinessTransaction[];
  contracts: Contract[];
}

export function createDemoDatabase(today = TODAY()): DemoDatabase {
  const next = random(20261012);
  const between = (low: number, high: number) => low + next() * (high - low);
  const pick = <T,>(items: T[]) => items[Math.floor(next() * items.length)];
  const todayIso = isoDate(today);
  const transactions: Transaction[] = [];
  let sequence = 0;
  const add = (account: string, date: Date, amount: number, counterparty: string, category: Category, description = "") => {
    const iso = isoDate(date);
    if (iso > todayIso) {
      return;
    }
    sequence += 1;
    transactions.push({
      id: `demo-${sequence}`,
      account,
      date: iso,
      amount: Math.round(amount * 100) / 100,
      currency: "EUR",
      counterparty,
      description,
      category,
    });
  };
  for (const month of months(today)) {
    const thisMonth = month.getMonth() === today.getMonth() && month.getFullYear() === today.getFullYear();
    for (const cost of FIXED_COSTS) {
      if (!cost.months || cost.months.includes(month.getMonth() + 1)) {
        const amount = thisMonth && cost.raisedTo ? cost.raisedTo : cost.amount;
        add("giro", day(month, cost.day), -amount, cost.counterparty, cost.category);
      }
    }
    add("giro", day(month, 27), 3140, "Brightline Software GmbH", "SALARY", "Gehalt");
    add("giro", day(month, 28), -300, "Tagesgeld", "TRANSFER", "Sparen");
    add("savings", day(month, 28), 300, "Girokonto", "TRANSFER", "Sparen");
    add("savings", day(month, 30), Math.round(between(18, 26) * 100) / 100, "Openbank", "INCOME", "Zinsen");
    add("giro", day(month, Math.round(between(1, 25))), -100, "Geldautomat", "CASH");
    for (const variable of VARIABLE) {
      const count = Math.round(between(variable.perMonth[0], variable.perMonth[1]));
      for (let index = 0; index < count; index += 1) {
        add("giro", day(month, Math.round(between(1, 28))), -between(variable.range[0], variable.range[1]), pick(variable.names), variable.category);
      }
    }
    const shopping = Math.round(between(1, 4));
    for (let index = 0; index < shopping; index += 1) {
      const date = day(month, Math.round(between(1, 28)));
      const amount = between(15, 140);
      add("paypal", date, -amount, pick(PAYPAL_SHOPS), "SHOPPING");
      add("paypal", date, amount, "Girokonto", "TRANSFER", "Aufladung");
      add("giro", date, -amount, "PayPal Europe S.a.r.l.", "TRANSFER");
    }
    if (month.getMonth() === 6) {
      add("giro", day(month, 9), -428, "Lufthansa", "TRAVEL", "Flug Lissabon");
      add("giro", day(month, 18), -612.5, "Booking.com", "TRAVEL", "Hotel");
    }
  }
  transactions.sort((first, second) => second.date.localeCompare(first.date));
  const contracts = demoContracts(today);
  return {
    connections: connections(today),
    transactions,
    recurring: recurring(today, transactions),
    businessConnections: demoBusinessConnections(today),
    businessTransactions: demoBusinessTransactions(today, contracts),
    contracts,
  };
}

function connections(today: Date): Connection[] {
  const synced = today.getTime() - 1000 * 60 * 47;
  const account = (id: string, name: string, iban: string | null, balance: number, spending = true): Account => ({
    id,
    name,
    iban,
    currency: "EUR",
    balance,
    last_synced_at: synced,
    sync_failure: null,
    spending,
  });
  return [
    {
      id: "openbank",
      bank_name: "Openbank",
      bank_country: "DE",
      valid_until: today.getTime() + 1000 * 60 * 60 * 24 * 142,
      accounts: [
        account("giro", "Girokonto", "DE89370400440532013000", 1284.15),
        account("savings", "Tagesgeld", "DE12500105170648489890", 12500, false),
      ],
    },
    {
      id: "paypal",
      bank_name: "PayPal",
      bank_country: "DE",
      valid_until: today.getTime() + 1000 * 60 * 60 * 24 * 11,
      accounts: [account("paypal", "PayPal", null, 86.4)],
    },
  ];
}

function recurring(today: Date, transactions: Transaction[]): RecurringPayment[] {
  return FIXED_COSTS.map((cost) => {
    const yearly = Boolean(cost.months);
    let last = day(today, cost.day);
    if (yearly) {
      last = new Date(today.getFullYear(), cost.months![0] - 1, cost.day);
      if (last > today) {
        last.setFullYear(last.getFullYear() - 1);
      }
    } else if (last > today) {
      last = day(new Date(today.getFullYear(), today.getMonth() - 1, 1), cost.day);
    }
    const next = yearly
      ? new Date(last.getFullYear() + 1, last.getMonth(), last.getDate())
      : day(new Date(last.getFullYear(), last.getMonth() + 1, 1), cost.day);
    const [latest, previous] = transactions.filter((entry) => entry.counterparty === cost.counterparty);
    const amount = latest ? -latest.amount : cost.amount;
    return {
      counterparty: cost.counterparty,
      category: cost.category,
      amount,
      previous_amount: previous ? -previous.amount : amount,
      currency: "EUR",
      cycle: yearly ? ("YEARLY" as const) : ("MONTHLY" as const),
      last_date: isoDate(last),
      next_date: isoDate(next),
      monthly_amount: Math.round((yearly ? amount / 12 : amount) * 100) / 100,
    };
  }).sort((first, second) => second.monthly_amount - first.monthly_amount);
}