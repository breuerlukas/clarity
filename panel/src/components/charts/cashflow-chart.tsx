import { useTranslation } from "react-i18next";
import {
  Bar,
  BarChart,
  CartesianGrid,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { ChartTooltip, Legend, Swatch } from "@/components/charts/chart-tooltip";
import { formatMoney, formatMonthShort, formatMonth, formatPercent } from "@/lib/format";
import type { MonthStatistic } from "@/lib/statistics";

/**
 * Money in above the zero line, money out below it, one pair per month. A
 * single axis, so in and out compare directly.
 */
export function CashflowChart({
  statistics,
  height,
  compact = false,
}: {
  statistics: MonthStatistic[];
  height: number;
  compact?: boolean;
}) {
  const { t } = useTranslation();
  const data = statistics.map((statistic) => ({
    ...statistic,
    out: -statistic.spending,
  }));
  return (
    <div className="grid gap-3">
      <Legend>
        <span className="flex items-center gap-2">
          <Swatch color="var(--series-in)" />
          {t("statistics.money_in")}
        </span>
        <span className="flex items-center gap-2">
          <Swatch color="var(--series-out)" />
          {t("statistics.money_out")}
        </span>
      </Legend>
      <div style={{ height }}>
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={data} stackOffset="sign" margin={{ top: 4, right: 0, bottom: 0, left: 0 }} barCategoryGap="22%">
            <CartesianGrid vertical={false} stroke="var(--divider)" />
            <XAxis
              dataKey="month"
              tickLine={false}
              axisLine={false}
              interval={compact ? "preserveStartEnd" : 0}
              tick={{ fill: "var(--text-muted)", fontSize: 12 }}
              tickFormatter={formatMonthShort}
            />
            <YAxis
              hide={compact}
              width={72}
              tickLine={false}
              axisLine={false}
              tick={{ fill: "var(--text-muted)", fontSize: 12 }}
              tickFormatter={(value: number) => formatMoney(value, "EUR", { compact: true })}
            />
            <ReferenceLine y={0} stroke="var(--text-muted)" strokeOpacity={0.4} />
            <Tooltip
              cursor={{ fill: "var(--raised)" }}
              content={({ active, payload }) => {
                const statistic = payload?.[0]?.payload as MonthStatistic | undefined;
                return active && statistic ? (
                  <ChartTooltip
                    title={formatMonth(statistic.month)}
                    rows={[
                      { label: t("statistics.money_in"), value: formatMoney(statistic.income), swatch: "var(--series-in)" },
                      { label: t("statistics.money_out"), value: formatMoney(statistic.spending), swatch: "var(--series-out)" },
                      { label: t("statistics.net"), value: formatMoney(statistic.net, "EUR", { signed: true }) },
                      ...(statistic.savingsRate !== null
                        ? [{ label: t("statistics.savings_rate"), value: formatPercent(statistic.savingsRate) }]
                        : []),
                    ]}
                  />
                ) : null;
              }}
            />
            <Bar dataKey="income" stackId="flow" fill="var(--series-in)" radius={[4, 4, 0, 0]} isAnimationActive={false} />
            <Bar dataKey="out" stackId="flow" fill="var(--series-out)" radius={[0, 0, 4, 4]} isAnimationActive={false} />
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}