import { ApiError, type CategoryDetails, type CategoryKind, type Transaction } from "@/lib/api";

/**
 * The built-in categories as the api seeds them, in the same order, with a
 * few of their keywords each; enough to show and reset them in the demo.
 */
const DEFAULTS: [key: string, icon: string, kind: CategoryKind, keywords: string[]][] = [
  ["SALARY", "briefcase", "INCOME", ["gehalt", "lohn", "salary"]],
  ["SUBSCRIPTIONS", "repeat", "SPENDING", ["amazon prime", "netflix", "spotify"]],
  ["HOUSING", "home", "SPENDING", ["miete", "hausverwaltung", "nebenkosten"]],
  ["UTILITIES", "plug", "SPENDING", ["stadtwerke", "telekom", "vodafone"]],
  ["GROCERIES", "shopping-cart", "SPENDING", ["rewe", "edeka", "lidl", "aldi"]],
  ["DINING", "utensils", "SPENDING", ["lieferando", "restaurant", "pizza"]],
  ["MOBILITY", "bus", "SPENDING", ["deutsche bahn", "mvg", "tankstelle"]],
  ["TRAVEL", "plane", "SPENDING", ["lufthansa", "booking.com", "hotel"]],
  ["SHOPPING", "shopping-bag", "SPENDING", ["amazon", "zalando", "ikea"]],
  ["INSURANCE", "shield-check", "SPENDING", ["versicherung", "allianz", "huk"]],
  ["HEALTH", "heart-pulse", "SPENDING", ["apotheke", "arzt", "physio"]],
  ["LEISURE", "ticket", "SPENDING", ["fitness", "kino", "museum"]],
  ["EDUCATION", "book-open", "SPENDING", ["udemy", "hochschule"]],
  ["CASH", "banknote", "SPENDING", ["geldautomat", "bargeld"]],
  ["FEES", "receipt", "SPENDING", ["kontoführung", "entgelt", "gebühr"]],
  ["SAVINGS", "piggy-bank", "SPENDING", ["trade republic", "sparplan"]],
  ["INCOME", "landmark", "INCOME", []],
  ["TRANSFER", "arrow-left-right", "SPENDING", []],
  ["OTHER", "circle-dashed", "SPENDING", []],
];

const SYSTEM = new Set(["SALARY", "INCOME", "SUBSCRIPTIONS", "TRANSFER", "CASH", "OTHER"]);

function shipped(key: string): CategoryDetails | undefined {
  const entry = DEFAULTS.find(([candidate]) => candidate === key);
  return entry && {
    key,
    name: null,
    icon: entry[1],
    color: "gray",
    kind: entry[2],
    keywords: entry[3],
    hidden: false,
    built_in: true,
    system: SYSTEM.has(key),
  };
}

export function demoCategories(): CategoryDetails[] {
  return DEFAULTS.map(([key]) => shipped(key)!);
}

const ok = (data: object = {}) => ({ success: true, ...data });

/** Checked like the api does, so the demo answers with the same codes. */
function draftOf(body: Record<string, unknown>, nameRequired: boolean) {
  const name = String(body.name ?? "").trim().slice(0, 40);
  const kind = String(body.kind) as CategoryKind;
  const keywords = [...new Set((body.keywords as string[] ?? []).map((keyword) => keyword.trimStart().toLowerCase()).filter((keyword) => keyword.trim()))];
  if ((nameRequired && !name) || !["SPENDING", "INCOME"].includes(kind) || keywords.some((keyword) => keyword.trim().length < 2)) {
    throw new ApiError(400, 3003);
  }
  return { name: name || null, icon: String(body.icon), color: String(body.color), kind, keywords };
}

function find(categories: CategoryDetails[], key: unknown) {
  const category = categories.find((entry) => entry.key === key);
  if (!category) {
    throw new ApiError(400, 3002);
  }
  return category;
}

export function createCategory(categories: CategoryDetails[], body: Record<string, unknown>) {
  const category: CategoryDetails = { key: crypto.randomUUID(), ...draftOf(body, true), hidden: false, built_in: false, system: false };
  categories.push(category);
  return ok({ category });
}

export function updateCategory(categories: CategoryDetails[], body: Record<string, unknown>) {
  const category = find(categories, body.key);
  if (body.hidden && category.system) {
    throw new ApiError(400, 3004);
  }
  const draft = draftOf(body, !category.built_in);
  Object.assign(category, draft, { kind: category.system ? category.kind : draft.kind, hidden: Boolean(body.hidden) });
  return ok({ category });
}

export function resetCategory(categories: CategoryDetails[], key: unknown) {
  const category = find(categories, key);
  const original = shipped(category.key);
  if (!original) {
    throw new ApiError(400, 3002);
  }
  Object.assign(category, original);
  return ok({ category });
}

/** Moves the transactions; the demo keeps no remembered rules. */
export function deleteCategory(categories: CategoryDetails[], transactions: Transaction[], key: unknown, target: unknown) {
  const category = find(categories, key);
  if (category.built_in) {
    throw new ApiError(400, 3004);
  }
  if (key === target || !categories.some((entry) => entry.key === target)) {
    throw new ApiError(400, 3005);
  }
  const moved = transactions.filter((transaction) => transaction.category === key);
  moved.forEach((transaction) => {
    transaction.category = String(target);
  });
  transactions.flatMap((transaction) => transaction.splits ?? []).forEach((part) => {
    if (part.category === key) {
      part.category = String(target);
    }
  });
  categories.splice(categories.indexOf(category), 1);
  return ok({ moved: moved.length });
}