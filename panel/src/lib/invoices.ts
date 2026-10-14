import type { TFunction } from "i18next";
import { ApiError, type Invoice, type InvoiceDirection, type InvoiceStatus } from "@/lib/api";
import type { MonthKey } from "@/lib/dates";
import { total } from "@/lib/overview";

/** Not yet paid in full: everything but PAID. */
export function isOpen(invoice: Invoice): boolean {
  return invoice.status !== "PAID";
}

/** Whom the invoice is with: the customer for one sent, the supplier for one received. */
export function counterpartyOf(invoice: Invoice): string {
  return invoice.direction === "OUTGOING" ? invoice.recipient : invoice.issuer;
}

/** The month an invoice is expected to be paid in: its due date's, else its date's. */
export function expectedMonth(invoice: Invoice): MonthKey {
  return (invoice.due_date ?? invoice.invoice_date).slice(0, 7);
}

export interface ExpectedActual {
  month: MonthKey;
  /** Gross of the invoices sent that are due this month (Soll). */
  expectedIncome: number;
  /** Payments booked this month on invoices sent (Ist). */
  actualIncome: number;
  /** Gross of the invoices received that are due this month. */
  expectedExpenses: number;
  /** Payments booked this month on invoices received. */
  actualExpenses: number;
}

/**
 * Expected against actual per month (Soll/Ist): what the invoices say should
 * come in and go out in a month, and what was paid on invoices in it.
 */
export function expectedVersusActual(invoices: Invoice[], months: MonthKey[]): ExpectedActual[] {
  const expected = (direction: InvoiceDirection, month: MonthKey) =>
    total(
      invoices
        .filter((invoice) => invoice.direction === direction && expectedMonth(invoice) === month)
        .map((invoice) => invoice.gross_amount),
    );
  const actual = (direction: InvoiceDirection, month: MonthKey) =>
    total(
      invoices
        .filter((invoice) => invoice.direction === direction)
        .flatMap((invoice) => invoice.payments)
        .filter((payment) => payment.date.slice(0, 7) === month)
        .map((payment) => payment.amount),
    );
  return months.map((month) => ({
    month,
    expectedIncome: expected("OUTGOING", month),
    actualIncome: actual("OUTGOING", month),
    expectedExpenses: expected("INCOMING", month),
    actualExpenses: actual("INCOMING", month),
  }));
}

export interface OpenSummary {
  /** Still to come in from customers. */
  receivables: number;
  /** Still to pay to suppliers. */
  payables: number;
  overdueReceivables: number;
  overduePayables: number;
  /** Invoices past their due date, both directions. */
  overdueCount: number;
  /** Invoices read from a plain PDF or a scan that nobody confirmed yet. */
  reviewCount: number;
}

export function openSummary(invoices: Invoice[]): OpenSummary {
  const open = (direction: InvoiceDirection, status?: InvoiceStatus) =>
    total(
      invoices
        .filter((invoice) => invoice.direction === direction && isOpen(invoice))
        .filter((invoice) => !status || invoice.status === status)
        .map((invoice) => invoice.open_amount),
    );
  return {
    receivables: open("OUTGOING"),
    payables: open("INCOMING"),
    overdueReceivables: open("OUTGOING", "OVERDUE"),
    overduePayables: open("INCOMING", "OVERDUE"),
    overdueCount: invoices.filter((invoice) => invoice.status === "OVERDUE").length,
    reviewCount: invoices.filter((invoice) => invoice.needs_review).length,
  };
}

const STATUS_ORDER: Record<InvoiceStatus, number> = {
  OVERDUE: 0,
  PARTIAL: 1,
  OPEN: 2,
  ANNOUNCED: 3,
  PAID: 4,
};

/**
 * Open ones first, the most urgent on top (overdue, then by due date),
 * paid ones after, the newest first.
 */
export function sortInvoices(invoices: Invoice[]): Invoice[] {
  const dueOf = (invoice: Invoice) => invoice.due_date ?? invoice.invoice_date;
  return [...invoices].sort(
    (first, second) =>
      STATUS_ORDER[first.status] - STATUS_ORDER[second.status] ||
      (isOpen(first)
        ? dueOf(first).localeCompare(dueOf(second))
        : second.invoice_date.localeCompare(first.invoice_date)),
  );
}

/** The message for an api error code of mailboxes and invoices, otherwise the general one. */
export function invoiceError(t: TFunction, error: Error): string {
  if (error instanceof ApiError && error.code === 9000) {
    return t("common.demo_only");
  }
  return error instanceof ApiError && error.code && t(`invoices.errors.${error.code}`, { defaultValue: "" })
    ? t(`invoices.errors.${error.code}`)
    : t("common.error");
}
