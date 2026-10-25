import { ApiError, type CashWallet, type Category, type Transaction } from "@/lib/api";
import type { DemoDatabase } from "@/demo/demo-data";
import { addDays, isoDate } from "@/lib/dates";

/**
 * The demo's cash: a wallet that started eight weeks ago, filled by the ATM
 * withdrawals since, with the bakery, the market and the odd flea market
 * sale paid in cash. A count at the start makes the balance believable.
 */
const STARTED_DAYS_AGO = 56;

/** One wallet per space, named after it, as the api starts them. */
export const walletId = (space: string) => `cash-${space}`;

const ok = (data: object = {}) => ({ success: true, ...data });

const CASH_SPENDING: [counterparty: string, category: Category, amount: number, everyDays: number][] = [
  ["Bäckerei Rischart", "GROCERIES", 3.9, 4],
  ["Viktualienmarkt", "GROCERIES", 14.8, 9],
  ["Kiosk am Gärtnerplatz", "OTHER", 2.5, 11],
  ["Eisdiele Ballabeni", "DINING", 5.6, 13],
  ["Friseur Haarscharf", "OTHER", 28, 40],
];

export function demoWallet(space: string, today: Date): CashWallet {
  return { id: walletId(space), space, balance: 0, currency: "EUR", started_on: isoDate(addDaysTo(today, -STARTED_DAYS_AGO)) };
}

export function demoCashEntries(space: string, today: Date, bankTransactions: Transaction[]): Transaction[] {
  const start = isoDate(addDaysTo(today, -STARTED_DAYS_AGO));
  const entries: Transaction[] = [];
  CASH_SPENDING.forEach(([counterparty, category, amount, everyDays], index) => {
    for (let day = STARTED_DAYS_AGO - index - 1; day > 0; day -= everyDays) {
      entries.push(entry(space, addDays(isoDate(today), -day), -amount, counterparty, category));
    }
  });
  entries.push(entry(space, addDays(isoDate(today), -20), 35, "Flohmarkt Theresienwiese", "INCOME", "Alte Lampe verkauft"));
  const withdrawn = bankTransactions
    .filter((transaction) => transaction.category === "CASH" && transaction.date >= start)
    .reduce((sum, transaction) => sum - transaction.amount, 0);
  const spent = entries.reduce((sum, transaction) => sum + transaction.amount, 0);
  entries.push(entry(space, start, Math.round((42.3 - withdrawn - spent) * 100) / 100, "", "TRANSFER"));
  return entries.sort((first, second) => second.date.localeCompare(first.date));
}

function entry(space: string, date: string, amount: number, counterparty: string, category: Category, note = ""): Transaction {
  return {
    id: crypto.randomUUID(),
    account: walletId(space),
    date,
    amount,
    currency: "EUR",
    counterparty,
    description: note,
    category,
    splits: [],
  };
}

function addDaysTo(date: Date, days: number) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate() + days);
}

/** The wallet of a space, started on first use like the api does. */
export function wallet(database: DemoDatabase, space: string): CashWallet {
  if (!database.spaces.some((entry) => entry.id === space)) {
    throw new ApiError(400, 5001);
  }
  let found = database.wallets.find((entry) => entry.space === space);
  if (!found) {
    found = demoWallet(space, new Date());
    found.started_on = isoDate(new Date());
    database.wallets.push(found);
  }
  return { ...found, balance: balance(database, found) };
}

function balance(database: DemoDatabase, cash: CashWallet) {
  const bankAccounts = new Set(
    database.connections
      .flatMap((connection) => connection.accounts)
      .filter((account) => account.space === cash.space && !account.hidden)
      .map((account) => account.id),
  );
  const cents = database.transactions.reduce((sum, transaction) => {
    if (transaction.account === cash.id) {
      return sum + Math.round(transaction.amount * 100);
    }
    const withdrawal = bankAccounts.has(transaction.account) && transaction.category === "CASH" && transaction.date >= cash.started_on;
    return withdrawal ? sum - Math.round(transaction.amount * 100) : sum;
  }, 0);
  return cents / 100;
}

export function countCash(database: DemoDatabase, space: string, counted: number) {
  const current = wallet(database, space);
  if (!Number.isFinite(counted) || counted < 0) {
    throw new ApiError(400, 8002);
  }
  const difference = Math.round((counted - current.balance) * 100) / 100;
  if (difference !== 0) {
    database.transactions.unshift(entry(space, isoDate(new Date()), difference, "", "TRANSFER"));
  }
  return ok({ wallet: wallet(database, space) });
}

/** The same checks as the api: a date, an amount other than zero with at most cents, a known category. */
function draftOf(database: DemoDatabase, body: Record<string, unknown>) {
  const amount = Number(body.amount);
  if (!/^\d{4}-\d{2}-\d{2}$/.test(String(body.date)) || !amount || Math.round(amount * 100) !== amount * 100) {
    throw new ApiError(400, 8000);
  }
  if (!database.categories.some((category) => category.key === body.category)) {
    throw new ApiError(400, 3002);
  }
  return {
    date: String(body.date),
    amount,
    counterparty: String(body.counterparty ?? "").trim(),
    description: String(body.note ?? "").trim(),
    category: String(body.category),
  };
}

function findEntry(database: DemoDatabase, id: string) {
  const found = database.transactions.find((transaction) => transaction.id === id && transaction.account.startsWith("cash-"));
  if (!found) {
    throw new ApiError(400, 8001);
  }
  return found;
}

export function createCashEntry(database: DemoDatabase, body: Record<string, unknown>) {
  const space = String(body.space);
  const created = { ...entry(space, "", 0, "", ""), ...draftOf(database, body), account: wallet(database, space).id };
  database.transactions.unshift(created);
  return ok({ transaction: created });
}

export function updateCashEntry(database: DemoDatabase, body: Record<string, unknown>) {
  const found = findEntry(database, String(body.id));
  const draft = draftOf(database, body);
  if (draft.amount !== found.amount) {
    found.splits = [];
  }
  Object.assign(found, draft);
  return ok({ transaction: found });
}

export function deleteCashEntry(database: DemoDatabase, id: string) {
  const found = findEntry(database, id);
  database.transactions.splice(database.transactions.indexOf(found), 1);
  database.receipts.forEach((receipt) => {
    if (receipt.transaction === id) {
      receipt.transaction = null;
    }
  });
  return ok();
}

/** Parts in the payment's direction with at most cents that add up to it exactly; none undo the split. */
export function splitTransaction(database: DemoDatabase, body: Record<string, unknown>) {
  const found = database.transactions.find((transaction) => transaction.id === body.id);
  if (!found) {
    throw new ApiError(400, 3001);
  }
  const parts = (body.splits as { category: string; amount: string }[]).map((part) => ({ category: part.category, amount: Number(part.amount) }));
  if (parts.some((part) => !database.categories.some((category) => category.key === part.category))) {
    throw new ApiError(400, 3002);
  }
  const cents = parts.map((part) => Math.round(part.amount * 100));
  const fits =
    parts.length === 0 ||
    (parts.length > 1 &&
      cents.every((value, index) => Math.abs(parts[index].amount * 100 - value) < 1e-6 && Math.sign(value) === Math.sign(found.amount)) &&
      cents.reduce((sum, value) => sum + value, 0) === Math.round(found.amount * 100));
  if (!fits) {
    throw new ApiError(400, 3006);
  }
  found.splits = parts;
  return ok({ splits: parts });
}
