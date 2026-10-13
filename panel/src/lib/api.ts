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
  /** The invoices this booking pays, with the part that goes to each. */
  invoices: { id: string; number: string; amount: number }[];
}

export type MailSecurity = "SSL" | "STARTTLS";

/** An IMAP mailbox of a business space; its password never comes back. */
export interface Mailbox {
  id: string;
  space: string;
  host: string;
  port: number;
  security: MailSecurity;
  username: string;
  /** Mails here are invoices to the business, and payment advices. */
  incoming_folder: string;
  /** Mails here are invoices the business sent; null watches none. */
  outgoing_folder: string | null;
  last_checked_at: number | null;
  /** Why the last check failed; null when it worked. */
  last_error: string | null;
  created_at: number;
}

export interface MailboxDraft {
  host: string;
  port: number;
  security: MailSecurity;
  username: string;
  /** Empty keeps the stored one of an existing mailbox. */
  password: string;
  incoming_folder: string;
  /** Empty watches no sent folder. */
  outgoing_folder: string;
}

/** INCOMING: the business pays it. OUTGOING: it sent it, a customer pays. */
export type InvoiceDirection = "INCOMING" | "OUTGOING";
export type InvoiceFormat = "XRECHNUNG" | "ZUGFERD" | "PDF" | "OCR";
export type InvoiceStatus = "OPEN" | "ANNOUNCED" | "PARTIAL" | "PAID" | "OVERDUE";

export interface InvoicePayment {
  id: string;
  transaction: string;
  /** The part of the transaction applied to this invoice, positive. */
  amount: number;
  /** Booking date of the transaction. */
  date: string;
  /** Linked by the automatic match rather than by hand. */
  automatic: boolean;
}

export interface Invoice {
  id: string;
  space: string;
  direction: InvoiceDirection;
  number: string;
  issuer: string;
  recipient: string;
  /** The account the money goes to (the issuer's), if the invoice names one. */
  iban: string | null;
  currency: string;
  net_amount: number | null;
  tax_amount: number | null;
  gross_amount: number;
  invoice_date: string;
  due_date: string | null;
  format: InvoiceFormat;
  /** Read from a plain PDF or a scan: fields may be wrong until confirmed. */
  needs_review: boolean;
  /** Marked as fully paid by hand or by an early-payment discount. */
  settled: boolean;
  /** The day a payment advice announced it. */
  announced_at: string | null;
  announced_amount: number | null;
  mail: { subject: string; sender: string; received_at: number } | null;
  document: { name: string; content_type: string; size: number } | null;
  payments: InvoicePayment[];
  paid_amount: number;
  /** Zero once settled. */
  open_amount: number;
  /** First that applies: PAID, OVERDUE, PARTIAL, ANNOUNCED, OPEN. */
  status: InvoiceStatus;
  created_at: number;
}

/** The fields the owner corrects, amounts and optional fields as text. */
export interface InvoiceDraft {
  direction: InvoiceDirection;
  number: string;
  issuer: string;
  recipient: string;
  /** Empty for none. */
  iban: string;
  currency: string;
  /** Empty when unknown. */
  net_amount: string;
  /** Empty when unknown. */
  tax_amount: string;
  gross_amount: string;
  invoice_date: string;
  /** Empty for none. */
  due_date: string;
}

export type MatchReason = "NUMBER" | "AMOUNT" | "IBAN" | "NAME";

export interface PaymentSuggestion {
  transaction: Transaction;
  reasons: MatchReason[];
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
  mailboxes: (space: string) =>
    get<{ mailboxes: Mailbox[] }>(`/business/mailboxes/?space=${space}`),
  mailFolders: (id: string | null, draft: MailboxDraft) =>
    post<{ folders: string[] }>("/business/mailboxes/folders/", { id: id ?? "", ...draft }),
  createMailbox: (space: string, draft: MailboxDraft) =>
    post<{ mailbox: Mailbox }>("/business/mailboxes/create/", { space, ...draft }),
  updateMailbox: (id: string, draft: MailboxDraft) =>
    post<{ mailbox: Mailbox }>("/business/mailboxes/update/", { id, ...draft }),
  deleteMailbox: (id: string) => post("/business/mailboxes/delete/", { id }),
  checkMailboxes: (space: string) =>
    post<{ started: boolean }>("/business/mailboxes/check/", { space }),
  invoices: (space: string) =>
    get<{ invoices: Invoice[] }>(`/business/invoices/?space=${space}`),
  updateInvoice: (id: string, draft: InvoiceDraft) =>
    post<{ invoice: Invoice }>("/business/invoices/update/", { id, ...draft }),
  settleInvoice: (id: string, settled: boolean) =>
    post<{ invoice: Invoice }>("/business/invoices/settle/", { id, settled }),
  deleteInvoice: (id: string) => post("/business/invoices/delete/", { id }),
  paymentSuggestions: (id: string) =>
    get<{ suggestions: PaymentSuggestion[] }>(`/business/invoices/suggestions/?id=${id}`),
  linkPayment: (invoice: string, transaction: string) =>
    post<{ invoice: Invoice }>("/business/invoices/payments/link/", { invoice, transaction }),
  unlinkPayment: (id: string) =>
    post<{ invoice: Invoice }>("/business/invoices/payments/unlink/", { id }),
};

/** Where the browser downloads an invoice's original file. */
export const invoiceDocumentUrl = (id: string) =>
  `/v1/business/invoices/document/?id=${encodeURIComponent(id)}`;