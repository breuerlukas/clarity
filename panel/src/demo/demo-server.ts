import {
  ApiError,
  type Category,
  type Contract,
  type ContractDraft,
  type OwnerSession,
  type PaymentKind,
} from "@/lib/api";
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
  await new Promise((resolve) => setTimeout(resolve, 120));
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
    case "GET /business/connections/":
      return ok({ connections: database.businessConnections });
    case "GET /business/transactions/":
      return ok({ transactions: businessTransactionsBetween(url.searchParams.get("from"), url.searchParams.get("to")) });
    case "POST /business/transactions/assign/":
      return assignPayment(String(body.id), String(body.kind), String(body.contract ?? ""));
    case "GET /business/contracts/":
      return ok({ contracts: database.contracts });
    case "POST /business/contracts/create/":
      return saveContract(null, body);
    case "POST /business/contracts/update/":
      return saveContract(findContract(String(body.id)), body);
    case "POST /business/contracts/end/":
      return endContract(findContract(String(body.id)), String(body.end_date));
    case "POST /business/contracts/resume/":
      return endContract(findContract(String(body.id)), null);
    case "POST /business/contracts/delete/":
      return deleteContract(findContract(String(body.id)));
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
  const list = database.connections.some((entry) => entry.id === id)
    ? database.connections
    : database.businessConnections;
  const connection = list.find((entry) => entry.id === id);
  if (!connection) {
    throw new ApiError(400, 2002);
  }
  const accounts = new Set(connection.accounts.map((account) => account.id));
  list.splice(list.indexOf(connection), 1);
  database.transactions = database.transactions.filter((entry) => !accounts.has(entry.account));
  database.businessTransactions = database.businessTransactions.filter((entry) => !accounts.has(entry.account));
  return ok();
}

function businessTransactionsBetween(from: string | null, to: string | null) {
  return database.businessTransactions.filter(
    (transaction) => (!from || transaction.date >= from) && (!to || transaction.date <= to),
  );
}

function assignPayment(id: string, kind: string, contract: string) {
  const transaction = database.businessTransactions.find((entry) => entry.id === id);
  if (!transaction) {
    throw new ApiError(400, 4003);
  }
  if (kind === "CONTRACT" && !database.contracts.some((entry) => entry.id === contract)) {
    throw new ApiError(400, 4001);
  }
  transaction.assignment =
    kind === "AUTO" ? null : { kind: kind as PaymentKind, contract: kind === "CONTRACT" ? contract : null };
  return ok();
}

function findContract(id: string) {
  const contract = database.contracts.find((entry) => entry.id === id);
  if (!contract) {
    throw new ApiError(400, 4001);
  }
  return contract;
}

/** The same checks as the api, so the demo refuses what the server would. */
function saveContract(contract: Contract | null, body: Record<string, unknown>) {
  const draft = body as unknown as ContractDraft;
  const customer = String(draft.customer ?? "").trim();
  const amount = Number(draft.amount);
  const valid =
    customer &&
    amount > 0 &&
    Math.round(amount * 100) === amount * 100 &&
    ["MONTHLY", "QUARTERLY", "HALF_YEARLY", "YEARLY"].includes(draft.interval) &&
    /^\d{4}-\d{2}-\d{2}$/.test(draft.start_date) &&
    draft.term_months >= 1 &&
    draft.term_months <= 120 &&
    draft.notice_months >= 0 &&
    draft.notice_months <= draft.term_months;
  if (!valid) {
    throw new ApiError(400, 4000);
  }
  const fields = {
    customer,
    amount,
    interval: draft.interval,
    start_date: draft.start_date,
    term_months: draft.term_months,
    notice_months: draft.notice_months,
  };
  if (contract) {
    Object.assign(contract, fields);
    return ok({ contract });
  }
  const created: Contract = { id: `contract-${Date.now()}`, ...fields, end_date: null, created_at: Date.now() };
  database.contracts.push(created);
  return ok({ contract: created });
}

function endContract(contract: Contract, endDate: string | null) {
  if (endDate !== null && endDate < contract.start_date) {
    throw new ApiError(400, 4002);
  }
  contract.end_date = endDate;
  return ok({ contract });
}

function deleteContract(contract: Contract) {
  database.contracts.splice(database.contracts.indexOf(contract), 1);
  database.businessTransactions.forEach((transaction) => {
    if (transaction.assignment?.contract === contract.id) {
      transaction.assignment = null;
    }
  });
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
