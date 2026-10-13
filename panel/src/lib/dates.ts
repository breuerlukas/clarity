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
const fromIso = (date: string) => {
  const [year, month, day] = date.split("-").map(Number);
  return { year, month, day };
};

/**
 * Calendar months later (or earlier), keeping the day where the month has
 * it: 31 January plus one month is 28 February.
 */
export function addMonths(date: string, months: number): string {
  const { year, month, day } = fromIso(date);
  const target = new Date(year, month - 1 + months, 1);
  const last = new Date(target.getFullYear(), target.getMonth() + 1, 0).getDate();
  return isoDate(new Date(target.getFullYear(), target.getMonth(), Math.min(day, last)));
}

export function addDays(date: string, days: number): string {
  const { year, month, day } = fromIso(date);
  return isoDate(new Date(year, month - 1, day + days));
}

/** Whole days from one date to another, negative when it lies before. */
export function daysBetween(from: string, to: string): number {
  const start = fromIso(from);
  const end = fromIso(to);
  return Math.round(
    (Date.UTC(end.year, end.month - 1, end.day) - Date.UTC(start.year, start.month - 1, start.day)) / 86_400_000,
  );
}
