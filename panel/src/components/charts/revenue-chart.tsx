import { useId } from "react";
import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { ChartTooltip } from "@/components/charts/chart-tooltip";
import type { RevenuePoint } from "@/lib/business";
import { formatMoney, formatMonth, formatMonthShort } from "@/lib/format";

/**
 * Monthly recurring revenue per month as one green line with a soft fill;
 * the tooltip adds the yearly figure and the number of customers.
 */
export function RevenueChart({
  data,
  height,
  labels,
}: {
  data: RevenuePoint[];
  height: number;
  labels: { mrr: string; arr: string; customers: string };
}) {
  const gradientId = useId();
  return (
    <div style={{ height }} role="img" aria-label={labels.mrr}>
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart data={data} margin={{ top: 6, right: 0, bottom: 0, left: 0 }}>
          <defs>
            <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="var(--brand)" stopOpacity={0.2} />
              <stop offset="100%" stopColor="var(--brand)" stopOpacity={0} />
            </linearGradient>
          </defs>
          <CartesianGrid vertical={false} stroke="var(--divider)" />
          <XAxis
            dataKey="month"
            tickLine={false}
            axisLine={false}
            minTickGap={24}
            tick={{ fill: "var(--text-muted)", fontSize: 12 }}
            tickFormatter={formatMonthShort}
          />
          <YAxis
            width={72}
            tickLine={false}
            axisLine={false}
            domain={[0, "auto"]}
            tick={{ fill: "var(--text-muted)", fontSize: 12 }}
            tickFormatter={(value: number) => formatMoney(value, "EUR", { compact: true })}
          />
          <Tooltip
            cursor={{ stroke: "var(--text-muted)", strokeWidth: 1 }}
            content={({ active, payload }) => {
              const point = payload?.[0]?.payload as RevenuePoint | undefined;
              return active && point ? (
                <ChartTooltip
                  title={formatMonth(point.month)}
                  rows={[
                    { label: labels.mrr, value: formatMoney(point.mrr) },
                    { label: labels.arr, value: formatMoney(point.arr) },
                    { label: labels.customers, value: String(point.customers) },
                  ]}
                />
              ) : null;
            }}
          />
          <Area
            type="monotone"
            dataKey="mrr"
            stroke="var(--brand)"
            strokeWidth={2}
            fill={`url(#${gradientId})`}
            dot={false}
            activeDot={{ r: 4, strokeWidth: 2, stroke: "var(--panel)", fill: "var(--brand)" }}
            isAnimationActive={false}
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
}
