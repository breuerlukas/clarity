import type {
  Account,
  BusinessTransaction,
  CashWallet,
  Category,
  CategoryDetails,
  Connection,
  Contract,
  Invoice,
  Mailbox,
  Receipt,
  RecurringPayment,
  RecurringStatus,
  SavingsGoal,
  Space,
  Transaction,
} from "@/lib/api";
import { demoCategories, type DemoBudget } from "@/demo/demo-categories";
import { isoDate } from "@/lib/dates";
import {
  BUSINESS_SPACE,
  demoBusinessConnections,
  demoBusinessTransactions,
  demoContracts,
} from "@/demo/demo-business";
import { demoInvoices, demoMailboxes } from "@/demo/demo-invoices";
import { demoCashEntries, demoWallet } from "@/demo/demo-cash";
import { demoReceipts } from "@/demo/demo-receipts";

/**
 * Made-up but believable finances for the demo: a salaried person in Munich
 * without a car, with a current account and a savings account at one bank
 * and PayPal. Everyday spending follows the week (no groceries on Sundays,
 * dinners out on weekends), amounts are skewed like real receipts (many
 * small, a few large), the year has its seasons, debits move off weekends,
 * and there are the one-offs every real account has. Seeded, so every
 * visitor sees the same history.
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
  description: string;
  /** Months (1-12) it is booked in; every month when missing. */
  months?: number[];
  /** The price since the current month, to show a price increase. */
  raisedTo?: number;
  /** Months without a booking up to now, for one that stopped. */
  stoppedFor?: number;
  /** Days of notice before it renews, to show the reminder. */
  noticeDays?: number;
}

const FIXED_COSTS: FixedCost[] = [
  { counterparty: "Hausverwaltung Schmidt GmbH", category: "HOUSING", amount: 1050, day: 1, description: "Miete Whg. 3. OG links" },
  { counterparty: "MVG Deutschlandticket", category: "MOBILITY", amount: 63, day: 1, description: "Deutschlandticket Abo" },
  { counterparty: "Trade Republic", category: "SAVINGS", amount: 250, day: 2, description: "Sparplan MSCI World" },
  { counterparty: "Netflix International B.V.", category: "SUBSCRIPTIONS", amount: 13.99, day: 3, description: "Netflix Standard", raisedTo: 15.99, noticeDays: 14 },
  { counterparty: "Spotify AB", category: "SUBSCRIPTIONS", amount: 10.99, day: 5, description: "Spotify Premium" },
  { counterparty: "Telekom Deutschland GmbH", category: "UTILITIES", amount: 39.95, day: 7, description: "MagentaZuhause M" },
  { counterparty: "Stadtwerke München", category: "UTILITIES", amount: 74, day: 10, description: "Abschlag Strom" },
  { counterparty: "McFit GmbH", category: "LEISURE", amount: 29.9, day: 15, description: "Mitgliedsbeitrag", stoppedFor: 2 },
  { counterparty: "Apple Services", category: "SUBSCRIPTIONS", amount: 2.99, day: 22, description: "iCloud+ 50 GB", stoppedFor: 3 },
  { counterparty: "ARD ZDF Deutschlandradio Beitragsservice", category: "UTILITIES", amount: 55.08, day: 15, months: [1, 4, 7, 10], description: "Rundfunkbeitrag" },
  { counterparty: "HUK-Coburg", category: "INSURANCE", amount: 289.4, day: 12, months: [1], description: "Hausrat- und Haftpflichtversicherung" },
  { counterparty: "Amazon Digital Germany", category: "SUBSCRIPTIONS", amount: 89.9, day: 18, months: [3], description: "Prime Jahresmitgliedschaft" },
];

/** A merchant, how often it is picked relative to the others, and its typical receipt. */
type Merchant = [name: string, weight: number, median: number];

interface Variable {
  category: Category;
  merchants: Merchant[];
  /** Average number of payments a month. */
  perMonth: number;
  /** How far receipts scatter around the median (log scale). */
  spread: number;
  /** Relative likelihood per weekday, Sunday first. */
  weekdays: number[];
  /** Opening hours for the card terminal's time stamp. */
  hours: [number, number];
  /** Extra activity per month (1-12), e.g. Christmas shopping. */
  season?: Partial<Record<number, number>>;
  /** Paid online through PayPal instead of by card. */
  paypal?: boolean;
}

const VARIABLE: Variable[] = [
  {
    category: "GROCERIES",
    merchants: [["REWE Markt GmbH", 5, 27], ["Lidl Dienstleistung", 3, 21], ["EDEKA Center", 2, 31], ["dm-drogerie markt", 2, 13], ["Alnatura", 1, 17], ["Bäckerei Rischart", 3, 4.8]],
    perMonth: 15,
    spread: 0.55,
    weekdays: [0, 1, 1, 1.1, 1.2, 1.6, 2.3],
    hours: [8, 20],
    season: { 12: 1.2, 8: 0.85 },
  },
  {
    category: "DINING",
    merchants: [["Lieferando.de", 3, 26], ["Starbucks Coffee", 2, 6.2], ["Vapiano", 1, 19], ["Wirtshaus am Markt", 1, 38], ["Augustiner Keller", 1, 31]],
    perMonth: 6,
    spread: 0.45,
    weekdays: [1.3, 0.7, 0.7, 0.9, 1, 1.9, 1.9],
    hours: [11, 22],
    season: { 1: 0.7, 6: 1.25, 7: 1.3, 8: 1.25, 12: 1.4 },
  },
  {
    category: "MOBILITY",
    merchants: [["TIER Mobility", 3, 3.6], ["FREE NOW", 1, 17], ["DB Fernverkehr AG", 1, 39]],
    perMonth: 2.5,
    spread: 0.6,
    weekdays: [0.8, 1, 1, 1, 1.1, 1.4, 1.2],
    hours: [7, 23],
  },
  {
    category: "HEALTH",
    merchants: [["Apotheke am Sendlinger Tor", 4, 11], ["Fielmann AG", 1, 34]],
    perMonth: 0.9,
    spread: 0.5,
    weekdays: [0, 1, 1, 1, 1, 1, 0.6],
    hours: [9, 19],
    season: { 1: 1.6, 2: 1.6, 11: 1.3, 12: 1.3 },
  },
  {
    category: "LEISURE",
    merchants: [["Boulderwelt München", 3, 11.5], ["Kino Mathäser", 2, 13.5], ["Steam Purchase", 1, 19.99], ["Eventim", 0.5, 64]],
    perMonth: 2.5,
    spread: 0.35,
    weekdays: [1.6, 0.8, 0.8, 1, 1, 1.4, 1.8],
    hours: [10, 22],
    season: { 6: 1.3, 7: 1.2, 8: 1.3 },
  },
  {
    category: "SHOPPING",
    merchants: [["AMAZON EU S.A R.L.", 4, 26], ["Zalando SE", 2, 59], ["Thalia", 1, 16], ["About You", 1, 44], ["IKEA Deutschland", 0.6, 85]],
    perMonth: 2.5,
    spread: 0.6,
    weekdays: [1.3, 1, 1, 1, 1, 0.9, 1.1],
    hours: [7, 23],
    season: { 1: 0.6, 11: 1.8, 12: 2.4 },
    paypal: true,
  },
];

/** Paid in an app or online, so the bank shows a Visa debit instead of a terminal. */
const ONLINE = new Set(["Lieferando.de", "TIER Mobility", "FREE NOW", "DB Fernverkehr AG", "Steam Purchase", "Eventim"]);

const TODAY = () => new Date();

function* months(today: Date) {
  for (let offset = HISTORY_MONTHS - 1; offset >= 0; offset -= 1) {
    yield { month: new Date(today.getFullYear(), today.getMonth() - offset, 1), offset };
  }
}

function day(month: Date, dayOfMonth: number) {
  const last = new Date(month.getFullYear(), month.getMonth() + 1, 0).getDate();
  return new Date(month.getFullYear(), month.getMonth(), Math.min(dayOfMonth, last));
}

/** Debits on a weekend are booked on the Monday after. */
function nextBankDay(date: Date) {
  const shift = date.getDay() === 6 ? 2 : date.getDay() === 0 ? 1 : 0;
  return new Date(date.getFullYear(), date.getMonth(), date.getDate() + shift);
}

/** Salaries due on a weekend arrive on the Friday before. */
function previousBankDay(date: Date) {
  const shift = date.getDay() === 6 ? 1 : date.getDay() === 0 ? 2 : 0;
  return new Date(date.getFullYear(), date.getMonth(), date.getDate() - shift);
}

function bookedMonths(cost: FixedCost) {
  return cost.months ?? [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12];
}

function twoDigits(value: number) {
  return String(value).padStart(2, "0");
}

export const PERSONAL_SPACE = "home";

export interface DemoDatabase {
  spaces: Space[];
  categories: CategoryDetails[];
  /** Of every space. */
  connections: Connection[];
  transactions: Transaction[];
  /** Of the personal space. */
  recurring: RecurringPayment[];
  businessTransactions: BusinessTransaction[];
  contracts: Contract[];
  invoices: Invoice[];
  mailboxes: Mailbox[];
  /** Started ones; the cash entries are among the transactions. */
  wallets: CashWallet[];
  receipts: Receipt[];
  budgets: DemoBudget[];
  goals: SavingsGoal[];
}

export function createDemoDatabase(today = TODAY()): DemoDatabase {
  const next = random(20261012);
  const between = (low: number, high: number) => low + next() * (high - low);
  const chance = (probability: number) => next() < probability;
  /** Normally distributed around 0 with a spread of 1 (Box-Muller). */
  const gauss = () => Math.sqrt(-2 * Math.log(1 - next())) * Math.cos(2 * Math.PI * next());
  /** Receipts: mostly near the median, now and then far above it. */
  const receipt = (median: number, spread: number) => Math.max(0.5, median * Math.exp(gauss() * spread));
  /** How many times something happens in a month with this average. */
  const occurrences = (average: number) => {
    let count = 0;
    for (let product = next(); product > Math.exp(-average); product *= next()) {
      count += 1;
    }
    return count;
  };
  const weighted = <T,>(items: T[], weight: (item: T) => number) => {
    let left = next() * items.reduce((sum, item) => sum + weight(item), 0);
    return items.find((item) => (left -= weight(item)) < 0) ?? items[items.length - 1];
  };
  const dayIn = (month: Date, weekdays: number[]) => {
    const length = new Date(month.getFullYear(), month.getMonth() + 1, 0).getDate();
    const days = Array.from({ length }, (_, index) => day(month, index + 1));
    return weighted(days, (date) => weekdays[date.getDay()]);
  };
  const cardPayment = (date: Date, [open, close]: [number, number]) =>
    `Kartenzahlung girocard ${twoDigits(date.getDate())}.${twoDigits(date.getMonth() + 1)}. ` +
    `${twoDigits(Math.floor(between(open, close)))}:${twoDigits(Math.floor(between(0, 60)))}`;

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
  /** Online orders: paid from PayPal, which tops itself up from the current account. */
  const paypal = (date: Date, amount: number, shop: string, category: Category) => {
    const order = `Bestellung ${Math.floor(between(100000, 999999))}`;
    add("paypal", date, -amount, shop, category, order);
    add("paypal", date, amount, "Girokonto", "TRANSFER", "Aufladung");
    add("giro", nextBankDay(date), -amount, "PayPal Europe S.a.r.l.", "TRANSFER", `PayPal ${shop}`);
    if (shop === "Zalando SE" && chance(0.35)) {
      const back = new Date(date.getFullYear(), date.getMonth(), date.getDate() + Math.round(between(6, 14)));
      add("paypal", back, amount, shop, category, `Retoure ${order}`);
    }
  };

  for (const { month, offset } of months(today)) {
    const calendarMonth = month.getMonth() + 1;
    const thisMonth = offset === 0;
    for (const cost of FIXED_COSTS) {
      if (bookedMonths(cost).includes(calendarMonth) && offset >= (cost.stoppedFor ?? 0)) {
        const amount = thisMonth && cost.raisedTo ? cost.raisedTo : cost.amount;
        add("giro", nextBankDay(day(month, cost.day)), -amount, cost.counterparty, cost.category, cost.description);
      }
    }

    // A raise five months ago, and the Christmas bonus with November's salary.
    const payday = previousBankDay(day(month, 27));
    const salary = offset <= 5 ? 3265.4 : 3140.15;
    add("giro", payday, salary, "Brightline Software GmbH", "SALARY", `Lohn/Gehalt ${twoDigits(calendarMonth)}/${month.getFullYear()}`);
    if (calendarMonth === 11) {
      add("giro", payday, 1418.62, "Brightline Software GmbH", "SALARY", "Weihnachtsgeld");
    }
    const saved = calendarMonth === 11 ? 1000 : 300;
    const transfer = nextBankDay(day(month, 28));
    add("giro", transfer, -saved, "Tagesgeld", "TRANSFER", "Sparen");
    add("savings", transfer, saved, "Girokonto", "TRANSFER", "Sparen");
    add("savings", previousBankDay(day(month, 31)), between(22, 27), "Openbank", "INCOME", "Zinsen");

    for (let index = occurrences(1.3); index > 0; index -= 1) {
      const date = dayIn(month, [0.3, 1, 1, 1, 1.2, 1.6, 1.2]);
      add("giro", date, -weighted([50, 100, 100, 200], () => 1), "Geldautomat Sparkasse", "CASH", cardPayment(date, [8, 23]));
    }

    for (const variable of VARIABLE) {
      const count = occurrences(variable.perMonth * (variable.season?.[calendarMonth] ?? 1));
      for (let index = 0; index < count; index += 1) {
        const [name, , median] = weighted(variable.merchants, ([, weight]) => weight);
        const date = dayIn(month, variable.weekdays);
        const amount = receipt(median, variable.spread);
        if (variable.paypal) {
          paypal(date, amount, name, variable.category);
        } else {
          const description = ONLINE.has(name)
            ? `VISA Debit ${twoDigits(date.getDate())}.${twoDigits(date.getMonth() + 1)}. ${name}`
            : cardPayment(date, variable.hours);
          add("giro", date, -amount, name, variable.category, description);
        }
      }
    }

    // Friends paying their share back.
    if (chance(0.45)) {
      const date = dayIn(month, [1.5, 0.6, 0.6, 0.6, 0.8, 1.2, 1.5]);
      const friend = weighted(["Lisa Hoffmann", "Jonas Becker", "Mehmet Kaya"], () => 1);
      add("paypal", date, Math.round(receipt(19, 0.4) * 2) / 2, friend, "INCOME", weighted(["Pizza", "Kino", "Geschenk für Anna", "Danke!"], () => 1));
    }

    // The things that only happen once in a while.
    if (calendarMonth === 2) {
      add("giro", nextBankDay(day(month, 20)), -143.27, "Stadtwerke München", "UTILITIES", `Nachzahlung Jahresabrechnung ${month.getFullYear() - 1}`);
    }
    if (calendarMonth === 4) {
      add("giro", day(month, 6), -386.4, "TAP Air Portugal", "TRAVEL", "Flug MUC-LIS-MUC");
    }
    if (calendarMonth === 7) {
      add("giro", day(month, 9), -612.5, "Booking.com", "TRAVEL", "Hotel Lissabon 10.-17.07.");
      add("giro", day(month, 11), -60, "Multibanco Lisboa", "CASH", "Geldautomat Ausland");
      add("giro", day(month, 11), -4.99, "Openbank", "FEES", "Fremdgebühr Geldautomat");
      for (const [dayOfMonth, place, amount] of [[10, "Time Out Market Lisboa", 34.6], [12, "Pastéis de Belém", 8.4], [14, "Cervejaria Ramiro", 71.2], [16, "Carris Lisboa", 6.6]] as const) {
        const date = day(month, dayOfMonth);
        add("giro", date, -amount, place, place === "Carris Lisboa" ? "MOBILITY" : "DINING", cardPayment(date, [11, 22]));
      }
    }
    if (calendarMonth === 8) {
      add("giro", day(month, 19), 612.34, "Finanzamt München", "INCOME", `Einkommensteuer ${month.getFullYear() - 1} Erstattung`);
    }
    if (calendarMonth === 10) {
      add("giro", day(month, 2), -89.8, "DB Fernverkehr AG", "TRAVEL", "ICE München-Hamburg");
      add("giro", day(month, 4), -178, "Motel One Hamburg", "TRAVEL", "2 Nächte");
    }
    if (calendarMonth === 11) {
      paypal(previousBankDay(day(month, 28)), 899, "MediaMarkt", "SHOPPING");
    }
    if (calendarMonth === 12) {
      add("giro", day(month, 20), 50, "Erika Wagner", "INCOME", "Frohe Weihnachten, mein Schatz!");
    }
    if (offset === 9) {
      add("giro", nextBankDay(day(month, 14)), -95, "Zahnarztpraxis Dr. Berger", "HEALTH", "Professionelle Zahnreinigung");
    }
    if (offset === 4) {
      add("paypal", day(month, 23), 45, "Kleinanzeigen", "INCOME", "Verkauf Stehlampe");
    }
  }
  transactions.push(...demoCashEntries(PERSONAL_SPACE, today, transactions));
  transactions.sort((first, second) => second.date.localeCompare(first.date));
  const receipts = demoReceipts(PERSONAL_SPACE, today, transactions);
  const contracts = demoContracts(today);
  const businessTransactions = demoBusinessTransactions(today, contracts);
  return {
    spaces: [
      { id: PERSONAL_SPACE, slug: "home", name: "", icon: "wallet", kind: "PERSONAL" },
      { id: BUSINESS_SPACE, slug: "agentur", name: "Agentur", icon: "palette", kind: "BUSINESS" },
    ],
    categories: demoCategories(),
    connections: [...connections(today), ...demoBusinessConnections(today)],
    transactions,
    recurring: recurring(today, transactions),
    businessTransactions,
    contracts,
    invoices: demoInvoices(today, businessTransactions),
    mailboxes: demoMailboxes(today),
    wallets: [demoWallet(PERSONAL_SPACE, today)],
    receipts,
    goals: [
      { id: "goal-holiday", space: PERSONAL_SPACE, name: "Sommerurlaub Portugal", icon: "plane", target: 1800, target_date: isoDate(new Date(today.getFullYear() + 1, 5, 1)), account: null, saved: 650, created_at: today.getTime() - 86_400_000 * 90 },
      { id: "goal-buffer", space: PERSONAL_SPACE, name: "Notgroschen", icon: "shield-check", target: 15000, target_date: null, account: "savings", saved: 0, created_at: today.getTime() - 86_400_000 * 200 },
      { id: "goal-laptop", space: PERSONAL_SPACE, name: "Neuer Laptop", icon: "smartphone", target: 1400, target_date: null, account: null, saved: 320, created_at: today.getTime() - 86_400_000 * 30 },
    ],
    budgets: [
      { space: PERSONAL_SPACE, category: "GROCERIES", limit: 450 },
      { space: PERSONAL_SPACE, category: "DINING", limit: 120 },
      { space: PERSONAL_SPACE, category: "LEISURE", limit: 80 },
      { space: PERSONAL_SPACE, category: "SHOPPING", limit: 150 },
    ],
  };
}

function connections(today: Date): Connection[] {
  const synced = today.getTime() - 1000 * 60 * 47;
  const account = (id: string, name: string, iban: string | null, balance: number, spending = true): Account => ({
    id,
    space: PERSONAL_SPACE,
    hidden: false,
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
      space: PERSONAL_SPACE,
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
      space: PERSONAL_SPACE,
      bank_name: "PayPal",
      bank_country: "DE",
      valid_until: today.getTime() + 1000 * 60 * 60 * 24 * 11,
      accounts: [account("paypal", "PayPal", null, 86.4)],
    },
  ];
}

const CYCLES: Record<number, RecurringPayment["cycle"]> = { 1: "YEARLY", 4: "QUARTERLY", 12: "MONTHLY" };

/** The longest gap each rhythm allows, as in the api's RecurringCycle. */
const MAXIMUM_DAYS: Record<RecurringPayment["cycle"], number> = { WEEKLY: 8, MONTHLY: 35, QUARTERLY: 98, YEARLY: 380 };

/**
 * Missing a week after the longest gap, ended a cycle later, gone after
 * one more; null once gone. Like the api's RecurringPayment.
 */
export function detectedStatus(payment: Pick<RecurringPayment, "cycle" | "last_date">, today: Date): RecurringStatus | null {
  const maximum = MAXIMUM_DAYS[payment.cycle];
  const overdue = (today.getTime() - new Date(`${payment.last_date}T12:00:00`).getTime()) / 86_400_000 - maximum - 7;
  if (overdue <= 0) {
    return "ACTIVE";
  }
  return overdue <= maximum ? "MISSING" : overdue <= 2 * maximum ? "ENDED" : null;
}

/** The fixed costs as the api would detect them from the bookings above. */
function recurring(today: Date, transactions: Transaction[]): RecurringPayment[] {
  return FIXED_COSTS.flatMap((cost) => {
    const booked = bookedMonths(cost);
    const [latest, previous] = transactions.filter(
      (entry) => entry.counterparty === cost.counterparty && entry.description === cost.description,
    );
    const last = latest ? new Date(`${latest.date}T12:00:00`) : today;
    let due = new Date(last.getFullYear(), last.getMonth() + 1, 1);
    while (!booked.includes(due.getMonth() + 1)) {
      due = new Date(due.getFullYear(), due.getMonth() + 1, 1);
    }
    const amount = latest ? -latest.amount : cost.amount;
    const cycle = CYCLES[booked.length];
    const nextDate = isoDate(nextBankDay(day(due, cost.day)));
    const status = detectedStatus({ cycle, last_date: isoDate(last) }, today);
    if (!status) {
      return [];
    }
    const cancelBy = new Date(`${nextDate}T12:00:00`);
    cancelBy.setDate(cancelBy.getDate() - (cost.noticeDays ?? 0));
    return {
      key: cost.counterparty.toLowerCase(),
      counterparty: cost.counterparty,
      category: cost.category,
      amount,
      previous_amount: previous ? -previous.amount : amount,
      currency: "EUR",
      cycle,
      last_date: isoDate(last),
      next_date: nextDate,
      monthly_amount: Math.round((amount * booked.length) / 12 * 100) / 100,
      status,
      cancelled_on: null,
      dismissed_last_date: null,
      ignored: false,
      name: null,
      notice_days: cost.noticeDays ?? null,
      cancel_by: cost.noticeDays === undefined ? null : isoDate(cancelBy),
    };
  }).sort((first, second) => second.monthly_amount - first.monthly_amount);
}
