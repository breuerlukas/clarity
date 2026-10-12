/** A calendar month as "2026-10", the way it appears in the URL. */
export type MonthKey = string;

const pad = (value: number) => String(value).padStart(2, "0");

export function isoDate(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

export function currentMonth(today = new Date()): MonthKey {
  return `${today.getFullYear()}-${pad(today.getMonth() + 1)}`;
}

export function monthRange(month: MonthKey): { from: string; to: string } {
  const [year, index] = month.split("-").map(Number);
  const last = new Date(year, index, 0).getDate();
  return { from: `${month}-01`, to: `${month}-${pad(last)}` };
}

export function shiftMonth(month: MonthKey, delta: number): MonthKey {
  const [year, index] = month.split("-").map(Number);
  const shifted = new Date(year, index - 1 + delta, 1);
  return currentMonth(shifted);
}

export function isValidMonth(value: string | null): value is MonthKey {
  return value !== null && /^\d{4}-(0[1-9]|1[0-2])$/.test(value);
}