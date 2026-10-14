import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import type { Transaction } from "@/lib/api";
import { isoDate } from "@/lib/dates";
import { formatDay, formatMoney, formatMonthShort } from "@/lib/format";
import { cn } from "@/lib/utils";

interface Day {
  date: string;
  income: number;
  spending: number;
}

const LEVELS = [14, 26, 40, 56, 72];

/**
 * One square per day, weeks as columns. The color says whether more came in
 * (green) or went out (blue) that day; the strength says how much, on a
 * square-root scale so salary and rent days don't wash out everything else.
 * Transfers between own accounts are left out. Leaving is handled on the
 * grid, not per square, so the label doesn't flicker across the gaps; the
 * label has its own fixed line, so nothing moves while hovering.
 */
export function ActivityHeatmap({
  transactions,
  from,
  to,
}: {
  transactions: Transaction[];
  from: string;
  to: string;
}) {
  const { t } = useTranslation();
  const [hovered, setHovered] = useState<Day | null>(null);
  const { weeks, maximum } = useMemo(() => layout(transactions, from, to), [transactions, from, to]);
  return (
    <div className="grid gap-3">
      <div className="-mx-1 overflow-x-auto px-1 pb-1">
        <div
          className="grid gap-[3px]"
          style={{ gridTemplateColumns: `repeat(${weeks.length}, minmax(8px, 1fr))` }}
          onMouseLeave={() => setHovered(null)}
        >
          {weeks.map((week, column) => (
            <div key={column} className="grid content-start gap-[3px]">
              <span className="relative h-4">
                {week.label && (
                  <span className="absolute left-0 text-[11px] whitespace-nowrap text-muted-foreground">
                    {formatMonthShort(week.label)}
                  </span>
                )}
              </span>
              {week.days.map((day, row) =>
                day ? (
                  <button
                    key={day.date}
                    type="button"
                    aria-label={`${formatDay(day.date)}: +${formatMoney(day.income)}, -${formatMoney(day.spending)}`}
                    onMouseEnter={() => setHovered(day)}
                    onFocus={() => setHovered(day)}
                    onBlur={() => setHovered(null)}
                    className={cn(
                      "aspect-square w-full rounded-[3px] outline-none transition-shadow duration-150",
                      hovered?.date === day.date && "ring-2 ring-foreground/70 ring-offset-1 ring-offset-card",
                    )}
                    style={{ backgroundColor: color(day, maximum) }}
                  />
                ) : (
                  <span key={`empty-${column}-${row}`} className="aspect-square w-full" />
                ),
              )}
            </div>
          ))}
        </div>
      </div>
      <div className="grid gap-2 text-xs text-muted-foreground">
        <span className="h-4 truncate" aria-live="polite">
          {hovered &&
            (hovered.income === 0 && hovered.spending === 0
              ? `${formatDay(hovered.date)}: ${t("statistics.activity_none")}`
              : `${formatDay(hovered.date)}: +${formatMoney(hovered.income)}, -${formatMoney(hovered.spending)}`)}
        </span>
        <span className="flex flex-wrap items-center gap-x-4 gap-y-1">
          <Scale label={t("statistics.activity_out")} color="var(--series-out)" />
          <Scale label={t("statistics.activity_in")} color="var(--series-in)" />
        </span>
      </div>
    </div>
  );
}

function Scale({ label, color }: { label: string; color: string }) {
  return (
    <span className="flex items-center gap-1.5">
      {label}
      {LEVELS.map((level) => (
        <span
          key={level}
          className="size-[9px] rounded-[2px]"
          style={{ backgroundColor: `color-mix(in oklab, ${color} ${level}%, transparent)` }}
        />
      ))}
    </span>
  );
}

function color(day: Day, maximum: number) {
  const net = day.income - day.spending;
  if (net === 0) {
    return "var(--raised)";
  }
  const strength = Math.sqrt(Math.abs(net)) / Math.sqrt(maximum);
  const level = LEVELS[Math.min(LEVELS.length - 1, Math.floor(strength * LEVELS.length))];
  return `color-mix(in oklab, ${net > 0 ? "var(--series-in)" : "var(--series-out)"} ${level}%, transparent)`;
}

/** Weeks from Monday to Sunday, each labeled with a month where one starts. */
function layout(transactions: Transaction[], from: string, to: string) {
  const days = new Map<string, Day>();
  for (const transaction of transactions) {
    if (transaction.category === "TRANSFER") {
      continue;
    }
    const day = days.get(transaction.date) ?? { date: transaction.date, income: 0, spending: 0 };
    if (transaction.amount > 0) {
      day.income += transaction.amount;
    } else {
      day.spending -= transaction.amount;
    }
    days.set(transaction.date, day);
  }
  const maximum = Math.max(1, ...[...days.values()].map((day) => Math.abs(day.income - day.spending)));
  const cursor = new Date(`${from}T12:00:00`);
  cursor.setDate(cursor.getDate() - ((cursor.getDay() + 6) % 7));
  const end = new Date(`${to}T12:00:00`);
  const weeks: { label: string | null; days: (Day | null)[] }[] = [];
  let lastMonth = "";
  while (cursor <= end) {
    const week: (Day | null)[] = [];
    let label: string | null = null;
    for (let weekday = 0; weekday < 7; weekday += 1) {
      const date = isoDate(cursor);
      if (date >= from && date <= to) {
        week.push(days.get(date) ?? { date, income: 0, spending: 0 });
        if (date.slice(0, 7) !== lastMonth) {
          lastMonth = date.slice(0, 7);
          label = label ?? lastMonth;
        }
      } else {
        week.push(null);
      }
      cursor.setDate(cursor.getDate() + 1);
    }
    weeks.push({ label, days: week });
  }
  return { weeks, maximum };
}
