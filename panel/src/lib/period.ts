import { currentMonth, isoDate, monthRange, shiftMonth, type MonthKey } from "@/lib/dates";
import { formatMonth } from "@/lib/format";

export type PeriodKind = "month" | "quarter" | "year" | "rolling";

/**
 * A stretch of time to look at: one month ("2026-10"), one quarter
 * ("2026-Q4"), one year ("2026") or the last twelve months ("12m"). It lives
 * in the URL as that string.
 */
export interface Period {
  kind: PeriodKind;
  key: string;
}

const pad = (value: number) => String(value).padStart(2, "0");

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
  }
}

export function parsePeriod(value: string | null, fallback: PeriodKind): Period {
  if (value === "12m") {
    return { kind: "rolling", key: value };
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

/** The months a period covers, oldest first. */
export function periodMonths(period: Period, today = new Date()): MonthKey[] {
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
  }
}

/** First and last day; never past today, there is nothing booked there yet. */
export function periodRange(period: Period, today = new Date()) {
  const months = periodMonths(period, today);
  const to = monthRange(months[months.length - 1]).to;
  const now = isoDate(today);
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
      return period;
  }
}

/** Whether the period is the current one, so there is no later one yet. */
export function isCurrentPeriod(period: Period, today = new Date()) {
  return period.kind === "rolling" || period.key === currentPeriod(period.kind, today).key;
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
  return period.key;
}
