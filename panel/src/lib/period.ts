import { addDays, currentMonth, isoDate, monthRange, shiftMonth, type MonthKey } from "@/lib/dates";
import { formatDate, formatMonth } from "@/lib/format";

export type PeriodKind = "month" | "quarter" | "year" | "rolling" | "custom" | "all";

/**
 * A stretch of time to look at: one month ("2026-10"), one quarter
 * ("2026-Q4"), one year ("2026"), the last twelve months ("12m"), days of
 * the owner's choice ("2026-03-15_2026-10-25") or everything there is
 * ("all"). It lives in the URL as that string.
 */
export interface Period {
  kind: PeriodKind;
  key: string;
}

const pad = (value: number) => String(value).padStart(2, "0");

/** How far back "all" reaches; the api answers ten years at most. */
const ALL_DAYS = 3650;
const DAY_PATTERN = /^\d{4}-\d{2}-\d{2}$/;

/** The days of a custom period, earlier one first whatever order they came in. */
export function customPeriod(from: string, to: string): Period {
  const [first, last] = from <= to ? [from, to] : [to, from];
  return { kind: "custom", key: `${first}_${last}` };
}

function customDays(period: Period): [string, string] {
  const [from, to] = period.key.split("_");
  return [from, to];
}

function monthsBetween(from: string, to: string): MonthKey[] {
  const months: MonthKey[] = [];
  for (let month = from.slice(0, 7); month <= to.slice(0, 7); month = shiftMonth(month, 1)) {
    months.push(month);
  }
  return months;
}

export function currentPeriod(kind: PeriodKind, today = new Date()): Period {
  const year = today.getFullYear();
  switch (kind) {
    case "month":
      return { kind, key: currentMonth(today) };
    case "quarter":
      return { kind, key: `${year}-Q${Math.floor(today.getMonth() / 3) + 1}` };
    case "year":
      return { kind, key: String(year) };
    case "rolling":
      return { kind, key: "12m" };
    case "custom":
      return customPeriod(isoDate(new Date(year, today.getMonth() - 2, 1)), isoDate(today));
    case "all":
      return { kind, key: "all" };
  }
}

export function parsePeriod(value: string | null, fallback: PeriodKind): Period {
  if (value === "12m") {
    return { kind: "rolling", key: value };
  }
  if (value === "all") {
    return { kind: "all", key: value };
  }
  const days = value?.split("_") ?? [];
  if (days.length === 2 && days.every((day) => DAY_PATTERN.test(day) && !Number.isNaN(Date.parse(day)))) {
    return customPeriod(days[0], days[1]);
  }
  if (value && /^\d{4}-(0[1-9]|1[0-2])$/.test(value)) {
    return { kind: "month", key: value };
  }
  if (value && /^\d{4}-Q[1-4]$/.test(value)) {
    return { kind: "quarter", key: value };
  }
  if (value && /^\d{4}$/.test(value)) {
    return { kind: "year", key: value };
  }
  return currentPeriod(fallback);
}

/**
 * The months a period covers, oldest first. "All" starts at the oldest
 * booking there is, not ten years back, so charts don't fill up with empty
 * months.
 */
export function periodMonths(period: Period, today = new Date(), oldest?: string): MonthKey[] {
  switch (period.kind) {
    case "month":
      return [period.key];
    case "quarter": {
      const [year, quarter] = period.key.split("-Q").map(Number);
      return [0, 1, 2].map((offset) => `${year}-${pad((quarter - 1) * 3 + offset + 1)}`);
    }
    case "year":
      return Array.from({ length: 12 }, (_, index) => `${period.key}-${pad(index + 1)}`);
    case "rolling": {
      const now = currentMonth(today);
      return Array.from({ length: 12 }, (_, index) => shiftMonth(now, index - 11));
    }
    case "custom":
      return monthsBetween(...customDays(period));
    case "all":
      return monthsBetween(oldest ?? isoDate(today), isoDate(today));
  }
}

/** First and last day; never past today, there is nothing booked there yet. */
export function periodRange(period: Period, today = new Date()) {
  const now = isoDate(today);
  if (period.kind === "all") {
    return { from: addDays(now, -ALL_DAYS), to: now };
  }
  if (period.kind === "custom") {
    const [from, to] = customDays(period);
    return { from: from > now ? now : from, to: to > now ? now : to };
  }
  const months = periodMonths(period, today);
  const to = monthRange(months[months.length - 1]).to;
  return { from: monthRange(months[0]).from, to: to > now ? now : to };
}

export function shiftPeriod(period: Period, delta: number): Period {
  switch (period.kind) {
    case "month":
      return { kind: "month", key: shiftMonth(period.key, delta) };
    case "quarter": {
      const [year, quarter] = period.key.split("-Q").map(Number);
      const index = year * 4 + (quarter - 1) + delta;
      return { kind: "quarter", key: `${Math.floor(index / 4)}-Q${(index % 4) + 1}` };
    }
    case "year":
      return { kind: "year", key: String(Number(period.key) + delta) };
    case "rolling":
    case "custom":
    case "all":
      return period;
  }
}

/** Whether the period is the current one, so there is no later one yet. */
export function isCurrentPeriod(period: Period, today = new Date()) {
  return ["rolling", "custom", "all"].includes(period.kind) || period.key === currentPeriod(period.kind, today).key;
}

/** "Oktober 2026", "Q4 2026", "2026" or the rolling key. */
export function periodLabel(period: Period) {
  if (period.kind === "month") {
    return formatMonth(period.key);
  }
  if (period.kind === "quarter") {
    const [year, quarter] = period.key.split("-");
    return `${quarter} ${year}`;
  }
  if (period.kind === "custom") {
    return customDays(period).map(formatDate).join(" – ");
  }
  return period.key;
}
