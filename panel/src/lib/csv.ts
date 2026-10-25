import type { Category, Transaction } from "@/lib/api";
import { categoryParts } from "@/lib/statistics";

export interface CsvOptions {
  /** German gets ";" and a decimal comma, so Excel opens it directly. */
  language: string;
  headers: string[];
  accountName: (id: string) => string;
  categoryName: (key: Category) => string;
  /** With a category filter only the matching parts of a split are exported. */
  category: Category | null;
}

/**
 * A spreadsheet can run text starting with these as a formula; bank data
 * comes from strangers, so such a cell gets a leading apostrophe.
 */
const FORMULA_START = /^[=+\-@\t\r]/;

/** Quoted when it holds either separator, a quote or a line break. */
function cell(value: string): string {
  const safe = FORMULA_START.test(value) ? `'${value}` : value;
  return /[";,\r\n]/.test(safe) ? `"${safe.replaceAll('"', '""')}"` : safe;
}

/**
 * The transactions as CSV, one row per part of a split one: date, account,
 * counterparty, purpose, amount, currency, category. Starts with a byte
 * order mark so Excel reads the umlauts as UTF-8.
 */
export function transactionsCsv(transactions: Transaction[], options: CsvOptions): string {
  const german = options.language.startsWith("de");
  const separator = german ? ";" : ",";
  const amount = (value: number) => (german ? value.toFixed(2).replace(".", ",") : value.toFixed(2));
  const rows = categoryParts(transactions)
    .filter((part) => !options.category || part.category === options.category)
    .map((part) =>
      [
        part.date,
        cell(options.accountName(part.account)),
        cell(part.counterparty),
        cell(part.description),
        amount(part.amount),
        part.currency,
        cell(options.categoryName(part.category)),
      ].join(separator),
    );
  return `\ufeff${[options.headers.map((header) => cell(header)).join(separator), ...rows].join("\r\n")}\r\n`;
}
