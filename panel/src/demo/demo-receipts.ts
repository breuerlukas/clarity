import { ApiError, type Receipt, type ReceiptItem, type Transaction } from "@/lib/api";
import type { DemoDatabase } from "@/demo/demo-data";
import { wallet } from "@/demo/demo-cash";
import { addDays, daysBetween, isoDate } from "@/lib/dates";

/**
 * Sample receipts: the last big supermarket shop, split into groceries and
 * drugstore items, a bakery receipt on a cash entry, and a market receipt
 * still waiting to be assigned. Scanning in the demo reads no photo; it
 * answers with the receipt of the latest grocery payment without one.
 */
const ok = (data: object = {}) => ({ success: true, ...data });

const SHOPPING_LIST: [name: string, share: number][] = [
  ["BIO BANANEN", 0.08],
  ["VOLLMILCH 3,5%", 0.06],
  ["BERGKÄSE", 0.14],
  ["VOLLKORNBROT", 0.1],
  ["TOMATEN RISPE", 0.09],
  ["ZAHNPASTA", 0.12],
  ["SPÜLMITTEL", 0.07],
];

const DRUGSTORE = new Set(["ZAHNPASTA", "SPÜLMITTEL"]);

/** Items that add up to the total, the last one taking the rounding. */
function itemsFor(total: number): ReceiptItem[] {
  const items = SHOPPING_LIST.map(([name, share]) => ({ name, amount: Math.round(total * share * 100) / 100 }));
  const rest = Math.round((total - items.reduce((sum, item) => sum + item.amount, 0)) * 100) / 100;
  return [...items, { name: "OBST & GEMÜSE", amount: rest }];
}

function receipt(space: string, transaction: Transaction | null, merchant: string, date: string, total: number, items: ReceiptItem[]): Receipt {
  return {
    id: crypto.randomUUID(),
    space,
    transaction: transaction?.id ?? null,
    merchant,
    date,
    total,
    items,
    file: { name: "kassenbon.jpg", content_type: "image/jpeg", size: 412_000 },
    created_at: new Date(`${date}T18:00:00`).getTime(),
  };
}

export function demoReceipts(space: string, today: Date, transactions: Transaction[]): Receipt[] {
  const receipts: Receipt[] = [];
  const shop = transactions.find((entry) => entry.counterparty === "REWE Markt GmbH" && entry.amount < -20);
  if (shop) {
    const items = itemsFor(-shop.amount);
    receipts.push(receipt(space, shop, "REWE Markt GmbH", shop.date, -shop.amount, items));
    const drugstore = items.filter((item) => DRUGSTORE.has(item.name)).reduce((sum, item) => sum + Math.round(item.amount * 100), 0);
    shop.splits = [
      { category: "GROCERIES", amount: (Math.round(shop.amount * 100) + drugstore) / 100 },
      { category: "HEALTH", amount: -drugstore / 100 },
    ];
  }
  const bakery = transactions.find((entry) => entry.counterparty === "Bäckerei Rischart" && entry.account.startsWith("cash-"));
  if (bakery) {
    receipts.push(receipt(space, bakery, "Bäckerei Rischart", bakery.date, -bakery.amount, [
      { name: "Brezn", amount: 1.2 },
      { name: "Kaffee", amount: Math.round((-bakery.amount - 1.2) * 100) / 100 },
    ]));
  }
  const yesterday = addDays(isoDate(today), -1);
  receipts.push(receipt(space, null, "Viktualienmarkt Obst Huber", yesterday, 9.4, [
    { name: "Erdbeeren 500g", amount: 4.5 },
    { name: "Äpfel 1kg", amount: 2.9 },
    { name: "Kräuter", amount: 2 },
  ]));
  return receipts.sort((first, second) => second.created_at - first.created_at);
}

function find(database: DemoDatabase, id: unknown) {
  const found = database.receipts.find((entry) => entry.id === id);
  if (!found) {
    throw new ApiError(400, 8101);
  }
  return found;
}

/** Without OCR in the browser: the latest grocery card payment that has no receipt yet. */
export function scanReceipt(database: DemoDatabase, body: Record<string, unknown>) {
  const space = String(body.space);
  wallet(database, space);
  const taken = new Set(database.receipts.map((entry) => entry.transaction));
  const payment = database.transactions.find(
    (entry) => entry.category === "GROCERIES" && entry.amount < -10 && !entry.account.startsWith("cash-") && !taken.has(entry.id),
  );
  const total = payment ? -payment.amount : 18.4;
  const scanned = receipt(space, null, payment?.counterparty ?? "REWE Markt GmbH", payment?.date ?? isoDate(new Date()), total, itemsFor(total));
  scanned.created_at = Date.now();
  scanned.file = { name: String(body.name ?? "receipt.jpg"), content_type: String(body.content_type ?? "image/jpeg"), size: 0 };
  database.receipts.unshift(scanned);
  return ok({ receipt: scanned });
}

export function updateReceipt(database: DemoDatabase, body: Record<string, unknown>) {
  const found = find(database, body.id);
  const total = body.total === "" ? null : Number(body.total);
  const items = (body.items as { name: string; amount: string }[]).map((item) => ({ name: item.name.trim(), amount: Number(item.amount) }));
  if ((total !== null && !(total > 0)) || items.some((item) => !item.name || !Number.isFinite(item.amount))) {
    throw new ApiError(400, 8102);
  }
  Object.assign(found, { merchant: String(body.merchant ?? "").trim(), date: body.date ? String(body.date) : null, total, items });
  return ok({ receipt: found });
}

export function deleteReceipt(database: DemoDatabase, id: unknown) {
  const found = find(database, id);
  database.receipts.splice(database.receipts.indexOf(found), 1);
  return ok();
}

/** Bank bookings of exactly the total from a day before to five days after, closest first. */
export function receiptSuggestions(database: DemoDatabase, id: unknown) {
  const found = find(database, id);
  if (!found.date || found.total === null) {
    return ok({ suggestions: [] });
  }
  const taken = new Set(database.receipts.map((entry) => entry.transaction));
  const date = found.date;
  const total = found.total;
  const suggestions = database.transactions
    .filter((entry) => !entry.account.startsWith("cash-") && !taken.has(entry.id))
    .filter((entry) => Math.round(-entry.amount * 100) === Math.round(total * 100))
    .filter((entry) => daysBetween(date, entry.date) >= -1 && daysBetween(date, entry.date) <= 5)
    .sort((first, second) => Math.abs(daysBetween(date, first.date)) - Math.abs(daysBetween(date, second.date)))
    .slice(0, 5);
  return ok({ suggestions });
}

export function attachReceipt(database: DemoDatabase, body: Record<string, unknown>) {
  const found = find(database, body.id);
  const transaction = database.transactions.find((entry) => entry.id === body.transaction);
  if (!transaction) {
    throw new ApiError(400, 8103);
  }
  found.transaction = transaction.id;
  return ok({ receipt: found, transaction });
}

export function bookReceiptAsCash(database: DemoDatabase, body: Record<string, unknown>) {
  const found = find(database, body.id);
  if (!database.categories.some((category) => category.key === body.category)) {
    throw new ApiError(400, 3002);
  }
  if (!found.date || found.total === null) {
    throw new ApiError(400, 8104);
  }
  const transaction: Transaction = {
    id: crypto.randomUUID(),
    account: wallet(database, found.space).id,
    date: found.date,
    amount: -found.total,
    currency: "EUR",
    counterparty: found.merchant,
    description: "",
    category: String(body.category),
    splits: [],
  };
  database.transactions.unshift(transaction);
  found.transaction = transaction.id;
  return ok({ receipt: found, transaction });
}
