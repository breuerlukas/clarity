import { ApiError, type Category, type OwnerSession } from "@/lib/api";
import { createDemoDatabase } from "@/demo/demo-data";

/**
 * Answers the panel's api calls in the browser for the public demo. Changes
 * (categories, a new name, removed banks) live in memory until reload; bank
 * logins and password changes are refused with code 9000.
 */
const database = createDemoDatabase();
let owner = "Demo";
let signedIn = true;
const sessions: OwnerSession[] = [
  { id: "this", ip_address: "127.0.0.1", platform: "", browser: "", mobile: false, opened_at: Date.now() - 3_600_000, last_seen_at: Date.now(), current: true },
  { id: "phone", ip_address: "100.64.0.7", platform: "iOS", browser: "Safari", mobile: true, opened_at: Date.now() - 86_400_000 * 3, last_seen_at: Date.now() - 7_200_000, current: false },
];

const DEMO_ONLY = 9000;
const ok = (data: object = {}) => ({ success: true, ...data });

export async function handleDemoRequest(
  method: "GET" | "POST",
  path: string,
  body: Record<string, unknown> = {},
): Promise<unknown> {
  // A server-like pause, long enough that the loading skeletons show.
  await new Promise((resolve) => setTimeout(resolve, 400 + Math.random() * 400));
  const url = new URL(path, "http://demo");
  const route = `${method} ${url.pathname}`;
  if (!signedIn && !route.startsWith("POST /login") && route !== "GET /setup/status/") {
    throw new ApiError(401);
  }
  switch (route) {
    case "GET /setup/status/":
      return ok({ setup_required: false });
    case "POST /login/":
      signedIn = true;
      return ok();
    case "POST /logout/":
      signedIn = false;
      return ok();
    case "GET /session/":
      return ok({ name: owner });
    case "POST /account/name/":
      owner = String(body.name);
      return ok();
    case "GET /sessions/":
      return ok({ sessions });
    case "POST /sessions/close/":
    case "POST /sessions/close-others/":
      sessions.splice(1);
      return ok({ closed: 1 });
    case "GET /connections/":
      return ok({ connections: database.connections });
    case "POST /accounts/spending/":
      return markSpending(String(body.id), Boolean(body.spending));
    case "POST /connections/sync/":
      return ok({ started: true });
    case "POST /connections/delete/":
      return removeConnection(String(body.id));
    case "GET /transactions/":
      return ok({ transactions: transactionsBetween(url.searchParams.get("from"), url.searchParams.get("to")) });
    case "POST /transactions/category/":
      return categorize(String(body.id), body.category as Category, Boolean(body.remember));
    case "GET /recurring/":
      return ok({ payments: database.recurring });
    case "GET /banks/":
      return ok({ banks: ["Openbank", "N26", "PayPal", "Sparkasse", "Deutsche Bank", "ING", "DKB", "Commerzbank", "comdirect", "Trade Republic"].map((name) => ({ name, country: url.searchParams.get("country"), maximum_consent_validity: 15552000 })) });
    default:
      throw new ApiError(400, DEMO_ONLY);
  }
}

function transactionsBetween(from: string | null, to: string | null) {
  return database.transactions.filter(
    (transaction) => (!from || transaction.date >= from) && (!to || transaction.date <= to),
  );
}

function categorize(id: string, category: Category, remember: boolean) {
  const transaction = database.transactions.find((entry) => entry.id === id);
  if (!transaction) {
    throw new ApiError(400, 3001);
  }
  transaction.category = category;
  const others = remember
    ? database.transactions.filter((entry) => entry.counterparty === transaction.counterparty && entry !== transaction)
    : [];
  others.forEach((entry) => {
    entry.category = category;
  });
  return ok({ updated: others.length });
}

function removeConnection(id: string) {
  const connection = database.connections.find((entry) => entry.id === id);
  if (!connection) {
    throw new ApiError(400, 2002);
  }
  const accounts = new Set(connection.accounts.map((account) => account.id));
  database.connections.splice(database.connections.indexOf(connection), 1);
  database.transactions = database.transactions.filter((entry) => !accounts.has(entry.account));
  return ok();
}

function markSpending(id: string, spending: boolean) {
  const account = database.connections.flatMap((connection) => connection.accounts).find((entry) => entry.id === id);
  if (!account) {
    throw new ApiError(400, 2004);
  }
  account.spending = spending;
  return ok();
}
