import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { ChartTooltip, Legend, Swatch } from "@/components/charts/chart-tooltip";
import type { ExpectedActual } from "@/lib/invoices";
import { formatMoney, formatMonth, formatMonthShort } from "@/lib/format";

export type ExpectedActualSide = "income" | "expenses";

const KEYS: Record<ExpectedActualSide, { expected: keyof ExpectedActual; actual: keyof ExpectedActual; color: string }> = {
  income: { expected: "expectedIncome", actual: "actualIncome", color: "var(--series-in)" },
  expenses: { expected: "expectedExpenses", actual: "actualExpenses", color: "var(--series-out)" },
};

/**
 * Expected (Soll) and actual (Ist) side by side per month: the expected bar
 * pale, the actual one solid in the same color, so a gap reads at a glance.
 */
export function ExpectedActualChart({
  data,
  side,
  height,
  labels,
}: {
  data: ExpectedActual[];
  side: ExpectedActualSide;
  height: number;
  labels: { expected: string; actual: string; difference: string };
}) {
  const keys = KEYS[side];
  const value = (point: ExpectedActual, key: keyof ExpectedActual) => point[key] as number;
  return (
    <div className="grid gap-3">
      <Legend>
        <span className="flex items-center gap-2">
          <span className="inline-block size-2.5 rounded-[3px] opacity-35" style={{ backgroundColor: keys.color }} aria-hidden />
          {labels.expected}
        </span>
        <span className="flex items-center gap-2">
          <Swatch color={keys.color} />
          {labels.actual}
        </span>
      </Legend>
      <div style={{ height }} role="img" aria-label={`${labels.expected} / ${labels.actual}`}>
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={data} margin={{ top: 4, right: 0, bottom: 0, left: 0 }} barCategoryGap="22%" barGap={2}>
            <CartesianGrid vertical={false} stroke="var(--divider)" />
            <XAxis
              dataKey="month"
              tickLine={false}
              axisLine={false}
              minTickGap={8}
              tick={{ fill: "var(--text-muted)", fontSize: 12 }}
              tickFormatter={formatMonthShort}
            />
            <YAxis
              width={72}
              tickLine={false}
              axisLine={false}
              tick={{ fill: "var(--text-muted)", fontSize: 12 }}
              tickFormatter={(amount: number) => formatMoney(amount, "EUR", { compact: true })}
            />
            <Tooltip
              cursor={{ fill: "var(--raised)" }}
              content={({ active, payload }) => {
                const point = payload?.[0]?.payload as ExpectedActual | undefined;
                return active && point ? (
                  <ChartTooltip
                    title={formatMonth(point.month)}
                    rows={[
                      { label: labels.expected, value: formatMoney(value(point, keys.expected)) },
                      { label: labels.actual, value: formatMoney(value(point, keys.actual)), swatch: keys.color },
                      {
                        label: labels.difference,
                        value: formatMoney(value(point, keys.actual) - value(point, keys.expected), "EUR", { signed: true }),
                      },
                    ]}
                  />
                ) : null;
              }}
            />
            <Bar dataKey={keys.expected} fill={keys.color} fillOpacity={0.35} radius={[4, 4, 0, 0]} isAnimationActive={false} />
            <Bar dataKey={keys.actual} fill={keys.color} radius={[4, 4, 0, 0]} isAnimationActive={false} />
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
