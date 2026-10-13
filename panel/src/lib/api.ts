export class ApiError extends Error {
  readonly status: number;
  readonly code?: number;

  constructor(status: number, code?: number) {
    super(`Request failed with status ${status}${code ? ` (${code})` : ""}`);
    this.status = status;
    this.code = code;
  }
}

type Payload = Record<string, unknown>;

/**
 * The session lives in an HttpOnly cookie the browser sends on its own; this
 * code never sees it. The custom header is what the api checks to tell
 * requests from this panel apart from forged cross-site ones.
 */
async function request<T>(
  method: "GET" | "POST",
  path: string,
  body?: Payload,
): Promise<T> {
  if (import.meta.env.VITE_DEMO === "true") {
    const { handleDemoRequest } = await import("@/demo/demo-server");
    return (await handleDemoRequest(method, path, body)) as T;
  }
  const response = await fetch(`/v1${path}`, {
    method,
    credentials: "same-origin",
    headers: {
      "X-Clarity-Request": "1",
      ...(body ? { "Content-Type": "application/json" } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  const data = await response.json().catch(() => null);
  if (!response.ok || !data?.success) {
    throw new ApiError(response.status, data?.error?.code);
  }
  return data as T;
}

const get = <T>(path: string) => request<T>("GET", path);
const post = <T>(path: string, body: Payload = {}) =>
  request<T>("POST", path, body);

export type Category =
  | "SALARY"
  | "INCOME"
  | "HOUSING"
  | "UTILITIES"
  | "GROCERIES"
  | "DINING"
  | "SHOPPING"
  | "MOBILITY"
  | "TRAVEL"
  | "SUBSCRIPTIONS"
  | "INSURANCE"
  | "HEALTH"
  | "LEISURE"
  | "EDUCATION"
  | "CASH"
  | "FEES"
  | "SAVINGS"
  | "TRANSFER"
  | "OTHER";

export interface Transaction {
  id: string;
  account: string;
  date: string;
  amount: number;
  currency: string;
  counterparty: string;
  description: string;
  category: Category;
  counterparty_iban?: string | null;
}

export interface Account {
  id: string;
  name: string;
  iban: string | null;
  currency: string;
  balance: number | null;
  last_synced_at: number;
  sync_failure: string | null;
  /** Counts towards the room left until payday. */
  spending: boolean;
}

export interface Connection {
  id: string;
  /** The space its accounts belong to. */
  space: string;
  bank_name: string;
  bank_country: string;
  valid_until: number;
  accounts: Account[];
}

export interface RecurringPayment {
  counterparty: string;
  category: Category;
  amount: number;
  previous_amount: number;
  currency: string;
  cycle: "WEEKLY" | "MONTHLY" | "QUARTERLY" | "YEARLY";
  last_date: string;
  next_date: string;
  monthly_amount: number;
}

export type BillingInterval = "MONTHLY" | "QUARTERLY" | "HALF_YEARLY" | "YEARLY";

/**
 * A customer contract. It renews by another term after each term unless it
 * was cancelled `notice_months` before; `end_date` is its last day.
 */
export interface Contract {
  id: string;
  space: string;
  customer: string;
  /** Per invoice, net. */
  amount: number;
  interval: BillingInterval;
  start_date: string;
  term_months: number;
  notice_months: number;
  end_date: string | null;
  created_at: number;
}

export type ContractDraft = Omit<Contract, "id" | "space" | "end_date" | "created_at">;

export type SpaceKind = "PERSONAL" | "BUSINESS";

/**
 * A separate set of banks (and, for a business, contracts). The first space
 * has no name until the owner gives it one.
 */
export interface Space {
  id: string;
  slug: string;
  name: string;
  icon: string;
  kind: SpaceKind;
}

/**
 * What a business booking is: a contract payment, a private withdrawal
 * (Privatentnahme), a private deposit (Einlage) or none of these.
 */
export type PaymentKind = "CONTRACT" | "WITHDRAWAL" | "DEPOSIT" | "NONE";

export interface BusinessTransaction extends Transaction {
  /** What the owner assigned by hand; null leaves it to the automatic match. */
  assignment: { kind: PaymentKind; contract: string | null } | null;
}

export interface Bank {
  name: string;
  country: string;
  maximum_consent_validity: number;
}

export interface OwnerSession {
  id: string;
  ip_address: string;
  platform: string;
  browser: string;
  mobile: boolean;
  opened_at: number;
  last_seen_at: number;
  current: boolean;
}

export interface SetupAnswer {
  totp_uri: string;
  totp_secret: string;
  qr_code: string;
}

export const api = {
  setupStatus: () => get<{ setup_required: boolean }>("/setup/status/"),
  setup: (setupCode: string, name: string, password: string) =>
    post<SetupAnswer>("/setup/", { setup_code: setupCode, name, password }),
  confirmSetup: (setupCode: string, code: string) =>
    post("/setup/confirm/", { setup_code: setupCode, code }),
  login: (name: string, password: string, code: string) =>
    post("/login/", { name, password, code }),
  logout: () => post("/logout/"),
  session: () => get<{ name: string }>("/session/"),
  rename: (name: string) => post("/account/name/", { name }),
  changePassword: (currentPassword: string, newPassword: string) =>
    post("/account/password/", {
      current_password: currentPassword,
      new_password: newPassword,
    }),
  sessions: () => get<{ sessions: OwnerSession[] }>("/sessions/"),
  closeSession: (id: string) => post("/sessions/close/", { id }),
  closeOtherSessions: () => post<{ closed: number }>("/sessions/close-others/"),
  spaces: () => get<{ spaces: Space[] }>("/spaces/"),
  createSpace: (name: string, icon: string, kind: SpaceKind) =>
    post<{ space: Space }>("/spaces/create/", { name, icon, kind }),
  updateSpace: (id: string, name: string, icon: string) =>
    post<{ space: Space }>("/spaces/update/", { id, name, icon }),
  deleteSpace: (id: string) => post("/spaces/delete/", { id }),
  transactions: (space: string, from: string, to: string) =>
    get<{ transactions: Transaction[] }>(
      `/transactions/?space=${space}&from=${from}&to=${to}`,
    ),
  categorize: (id: string, category: Category, remember: boolean) =>
    post<{ updated: number }>("/transactions/category/", {
      id,
      category,
      remember,
    }),
  recurring: (space: string) =>
    get<{ payments: RecurringPayment[] }>(`/recurring/?space=${space}`),
  connections: () => get<{ connections: Connection[] }>("/connections/"),
  markSpending: (id: string, spending: boolean) =>
    post("/accounts/spending/", { id, spending }),
  synchronize: () => post<{ started: boolean }>("/connections/sync/"),
  removeConnection: (id: string) => post("/connections/delete/", { id }),
  banks: (country: string) =>
    get<{ banks: Bank[] }>(`/banks/?country=${encodeURIComponent(country)}`),
  startConnection: (bank: Bank, space: string) =>
    post<{ url: string }>("/connections/start/", {
      bank: bank.name,
      country: bank.country,
      maximum_consent_validity: bank.maximum_consent_validity,
      space,
    }),
  completeConnection: (code: string, state: string) =>
    post<{ space: string }>("/connections/complete/", { code, state }),
  businessTransactions: (space: string, from: string, to: string) =>
    get<{ transactions: BusinessTransaction[] }>(
      `/business/transactions/?space=${space}&from=${from}&to=${to}`,
    ),
  assignPayment: (id: string, kind: PaymentKind | "AUTO", contract?: string) =>
    post("/business/transactions/assign/", { id, kind, contract: contract ?? "" }),
  contracts: (space: string) =>
    get<{ contracts: Contract[] }>(`/business/contracts/?space=${space}`),
  createContract: (space: string, draft: ContractDraft) =>
    post<{ contract: Contract }>("/business/contracts/create/", { space, ...draft }),
  updateContract: (id: string, draft: ContractDraft) =>
    post<{ contract: Contract }>("/business/contracts/update/", { id, ...draft }),
  endContract: (id: string, endDate: string) =>
    post<{ contract: Contract }>("/business/contracts/end/", { id, end_date: endDate }),
  resumeContract: (id: string) =>
    post<{ contract: Contract }>("/business/contracts/resume/", { id }),
  deleteContract: (id: string) => post("/business/contracts/delete/", { id }),
};