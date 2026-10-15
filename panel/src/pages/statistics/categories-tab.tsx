import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { Panel } from "@/components/panel";
import { CategoryIcon } from "@/components/category-icon";
import { useCategoryLook } from "@/hooks/use-categories";
import { ChartTooltip } from "@/components/charts/chart-tooltip";
import { CategoryDonut } from "@/components/charts/category-donut";
import type { Category, Transaction } from "@/lib/api";
import type { MonthKey } from "@/lib/dates";
import { formatMoney, formatMonth, formatMonthShort, formatPercent } from "@/lib/format";
import { categoryMatrix, topCounterparties } from "@/lib/statistics";
import { cn } from "@/lib/utils";

const TOP = 5;

/**
 * The categories as a ranked list beside the split as a ring; the selected
 * one gets its own monthly chart and the places that money went to. One
 * category at a time instead of a wall of numbers.
 */
export function CategoriesTab({
  transactions,
  months,
}: {
  transactions: Transaction[];
  months: MonthKey[];
}) {
  const { t } = useTranslation();
  const look = useCategoryLook();
  const rows = categoryMatrix(transactions, months);
  const [chosen, setChosen] = useState<Category | null>(null);
  const selected = rows.find((row) => row.category === chosen) ?? rows[0];
  const spending = rows.reduce((sum, row) => sum + row.total, 0);
  if (!selected) {
    return <Panel><p className="text-muted-foreground">{t("overview.categories_empty")}</p></Panel>;
  }
  const merchants = topCounterparties(
    transactions.filter((transaction) => transaction.category === selected.category),
    TOP,
  );
  return (
    <div className="grid gap-6 xl:grid-cols-[minmax(0,420px)_minmax(0,1fr)]">
      {/* Wide screens: as tall as the right column, the list scrolls inside. */}
      <div className="relative min-w-0">
        <Panel title={t("statistics.categories")} className="xl:absolute xl:inset-0">
          <ul className="grid content-start gap-0.5 xl:-mx-2 xl:min-h-0 xl:flex-1 xl:overflow-y-auto xl:px-2">
            {rows.map((row) => (
              <li key={row.category}>
                <button
                  type="button"
                  aria-pressed={row.category === selected.category}
                  onClick={() => setChosen(row.category)}
                  className={cn(
                    "grid w-full grid-cols-[auto_1fr_auto] items-center gap-3 rounded-xl px-2 py-2 text-left hover:bg-raised",
                    row.category === selected.category && "bg-brand/12",
                  )}
                >
                  <CategoryIcon category={row.category} />
                  <span className="grid min-w-0">
                    <span className={cn("truncate text-[15px]", row.category === selected.category && "font-extrabold text-nav-active-text")}>
                      {look(row.category).label}
                    </span>
                    <span className="text-xs text-muted-foreground">
                      {t("statistics.per_month", { amount: formatMoney(row.average) })}
                    </span>
                  </span>
                  <span className="grid justify-items-end gap-0.5">
                    <span className="figures text-[15px] font-bold">{formatMoney(row.total)}</span>
                    <span className="rounded-full bg-raised px-1.5 text-[11px] font-bold text-muted-foreground ring-1 ring-divider">
                      {formatPercent(row.total / spending)}
                    </span>
                  </span>
                </button>
              </li>
            ))}
          </ul>
        </Panel>
      </div>
      <div className="flex min-w-0 flex-col gap-6">
        <Panel title={t("statistics.split")}>
          <CategoryDonut rows={rows} onSelect={setChosen} />
        </Panel>
        <Panel title={look(selected.category).label}>
          <p className="-mt-3 truncate text-sm text-muted-foreground">
            {t("statistics.category_summary", {
              total: formatMoney(selected.total),
              average: formatMoney(selected.average),
            })}
          </p>
          <div style={{ height: 280 }}>
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={months.map((month, index) => ({ month, amount: selected.amounts[index] }))} margin={{ top: 4, right: 0, bottom: 0, left: 0 }}>
                <CartesianGrid vertical={false} stroke="var(--divider)" />
                <XAxis dataKey="month" tickLine={false} axisLine={false} tick={{ fill: "var(--text-muted)", fontSize: 12 }} tickFormatter={formatMonthShort} />
                <YAxis width={72} tickLine={false} axisLine={false} tick={{ fill: "var(--text-muted)", fontSize: 12 }} tickFormatter={(value: number) => formatMoney(value, "EUR", { compact: true })} />
                <Tooltip
                  cursor={{ fill: "var(--raised)" }}
                  content={({ active, payload }) =>
                    active && payload?.[0] ? (
                      <ChartTooltip
                        title={formatMonth(String(payload[0].payload.month))}
                        rows={[{ label: look(selected.category).label, value: formatMoney(Number(payload[0].value)), swatch: "var(--series-out)" }]}
                      />
                    ) : null
                  }
                />
                <Bar dataKey="amount" fill="var(--series-out)" radius={[4, 4, 0, 0]} isAnimationActive={false} />
              </BarChart>
            </ResponsiveContainer>
          </div>
          <div className="grid gap-2 border-t border-divider pt-4">
            <h3 className="text-sm font-bold text-label">{t("statistics.top_in_category")}</h3>
            <ul className="grid gap-1.5">
              {merchants.map((merchant) => (
                <li key={merchant.name} className="flex items-baseline justify-between gap-4 text-[15px]">
                  <span className="truncate">{merchant.name}</span>
                  <span className="flex shrink-0 items-baseline gap-3">
                    <span className="text-xs text-muted-foreground">{t("statistics.payments", { count: merchant.count })}</span>
                    <span className="figures font-bold">{formatMoney(merchant.amount)}</span>
                  </span>
                </li>
              ))}
              {/* Always room for the full list, so switching categories doesn't move the page. */}
              {Array.from({ length: TOP - merchants.length }, (_, index) => (
                <li key={`empty-${index}`} className="text-[15px]" aria-hidden="true">&nbsp;</li>
              ))}
            </ul>
          </div>
        </Panel>
      </div>
    </div>
  );
}
