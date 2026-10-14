import { describe, expect, it } from "vitest";
import type { Invoice, InvoicePayment } from "@/lib/api";
import {
  counterpartyOf,
  expectedMonth,
  expectedVersusActual,
  openSummary,
  sortInvoices,
} from "@/lib/invoices";

const invoice = (overrides: Partial<Invoice> = {}): Invoice => ({
  id: crypto.randomUUID(),
  space: "agency",
  direction: "OUTGOING",
  number: "RE-2026-001",
  issuer: "Agentur",
  recipient: "Müller GmbH",
  iban: null,
  currency: "EUR",
  net_amount: 1000,
  tax_amount: 190,
  gross_amount: 1190,
  invoice_date: "2026-09-20",
  due_date: "2026-10-04",
  format: "XRECHNUNG",
  needs_review: false,
  settled: false,
  announced_at: null,
  announced_amount: null,
  mail: null,
  document: null,
  payments: [],
  paid_amount: 0,
  open_amount: 1190,
  status: "OPEN",
  created_at: 0,
  ...overrides,
});

const payment = (amount: number, date: string): InvoicePayment => ({
  id: crypto.randomUUID(),
  transaction: crypto.randomUUID(),
  amount,
  date,
  automatic: true,
});

describe("invoices", () => {
  it("expects payment in the due month, else in the invoice's", () => {
    expect(expectedMonth(invoice())).toBe("2026-10");
    expect(expectedMonth(invoice({ due_date: null }))).toBe("2026-09");
  });

  it("names the customer of a sent invoice and the supplier of a received one", () => {
    expect(counterpartyOf(invoice())).toBe("Müller GmbH");
    expect(counterpartyOf(invoice({ direction: "INCOMING", issuer: "Hetzner" }))).toBe("Hetzner");
  });

  it("compares expected and actual per month", () => {
    const invoices = [
      invoice({ payments: [payment(500, "2026-10-02"), payment(690, "2026-11-01")] }),
      invoice({ gross_amount: 238, due_date: "2026-10-30", payments: [] }),
      invoice({ direction: "INCOMING", gross_amount: 64.9, due_date: "2026-10-03", payments: [payment(64.9, "2026-09-30")] }),
    ];
    const [september, october, november] = expectedVersusActual(invoices, ["2026-09", "2026-10", "2026-11"]);
    expect(october).toEqual({
      month: "2026-10",
      expectedIncome: 1428,
      actualIncome: 500,
      expectedExpenses: 64.9,
      actualExpenses: 0,
    });
    expect(september.actualExpenses).toBe(64.9);
    expect(september.expectedIncome).toBe(0);
    expect(november.actualIncome).toBe(690);
  });

  it("sums what is open, overdue and to review", () => {
    const summary = openSummary([
      invoice({ status: "PARTIAL", open_amount: 690, paid_amount: 500 }),
      invoice({ status: "OVERDUE", open_amount: 238 }),
      invoice({ status: "PAID", open_amount: 0 }),
      invoice({ direction: "INCOMING", status: "OVERDUE", open_amount: 120.5, needs_review: true }),
      invoice({ direction: "INCOMING", status: "ANNOUNCED", open_amount: 80 }),
    ]);
    expect(summary).toEqual({
      receivables: 928,
      payables: 200.5,
      overdueReceivables: 238,
      overduePayables: 120.5,
      overdueCount: 2,
      reviewCount: 1,
    });
  });

  it("puts overdue first and paid last", () => {
    const paidOld = invoice({ status: "PAID", invoice_date: "2026-01-01" });
    const paidNew = invoice({ status: "PAID", invoice_date: "2026-09-01" });
    const openLate = invoice({ status: "OPEN", due_date: "2026-11-01" });
    const openSoon = invoice({ status: "OPEN", due_date: "2026-10-20" });
    const overdue = invoice({ status: "OVERDUE" });
    expect(sortInvoices([paidOld, openLate, paidNew, overdue, openSoon])).toEqual([
      overdue,
      openSoon,
      openLate,
      paidNew,
      paidOld,
    ]);
  });
});
