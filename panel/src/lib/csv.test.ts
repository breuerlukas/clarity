import { describe, expect, it } from "vitest";
import type { Transaction } from "@/lib/api";
import { transactionsCsv } from "@/lib/csv";

const transaction = (overrides: Partial<Transaction>) =>
  ({
    id: "1",
    account: "giro",
    date: "2026-10-01",
    amount: -12.5,
    currency: "EUR",
    counterparty: "REWE",
    description: "",
    category: "GROCERIES",
    ...overrides,
  }) as Transaction;

const options = {
  headers: ["Date", "Account", "Counterparty", "Purpose", "Amount", "Currency", "Category"],
  accountName: (id: string) => (id === "giro" ? "Giro" : "Cash"),
  categoryName: (key: string) => key.toLowerCase(),
  category: null,
};

const lines = (csv: string) => csv.replace(/^\ufeff/, "").split("\r\n");

describe("transactionsCsv", () => {
  it("uses ; and a decimal comma in German and quotes what needs it", () => {
    const csv = transactionsCsv([transaction({ description: 'Ref; "A"\nline 2' })], { ...options, language: "de" });
    expect(csv.startsWith("\ufeff")).toBe(true);
    expect(lines(csv)[1]).toBe('2026-10-01;Giro;REWE;"Ref; ""A""\nline 2";-12,50;EUR;groceries');
  });

  it("uses , and a decimal point in English and defuses formulas", () => {
    const csv = transactionsCsv([transaction({ counterparty: "=HYPERLINK(1)", description: "a,b" })], { ...options, language: "en" });
    expect(lines(csv)[1]).toBe(`2026-10-01,Giro,'=HYPERLINK(1),"a,b",-12.50,EUR,groceries`);
  });

  it("writes one row per split part, only matching parts under a filter", () => {
    const split = transaction({
      amount: -30,
      splits: [
        { category: "GROCERIES", amount: -20 },
        { category: "HEALTH", amount: -10 },
      ],
    });
    expect(lines(transactionsCsv([split], { ...options, language: "en" })).slice(1, -1)).toEqual([
      "2026-10-01,Giro,REWE,,-20.00,EUR,groceries",
      "2026-10-01,Giro,REWE,,-10.00,EUR,health",
    ]);
    expect(lines(transactionsCsv([split], { ...options, language: "en", category: "HEALTH" })).slice(1, -1)).toEqual([
      "2026-10-01,Giro,REWE,,-10.00,EUR,health",
    ]);
  });
});
