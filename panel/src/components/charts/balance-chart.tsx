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
import { formatDate, formatMoney } from "@/lib/format";

type Point = { date: string; balance: number };

/**
 * The balance over time as one green line with a soft fill. `bare` drops
 * axes and grid for the sparkline next to the big number.
 */
export function BalanceChart({
  data,
  height,
  bare = false,
  label,
}: {
  data: Point[];
  height: number;
  bare?: boolean;
  label: string;
}) {
  const gradientId = useId();
  return (
    <div style={{ height }} role="img" aria-label={label}>
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart data={data} margin={{ top: 6, right: 0, bottom: 0, left: 0 }}>
          <defs>
            <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="var(--brand)" stopOpacity={0.2} />
              <stop offset="100%" stopColor="var(--brand)" stopOpacity={0} />
            </linearGradient>
          </defs>
          {!bare && <CartesianGrid vertical={false} stroke="var(--divider)" />}
          <XAxis
            dataKey="date"
            hide={bare}
            tickLine={false}
            axisLine={false}
            minTickGap={48}
            tick={{ fill: "var(--text-muted)", fontSize: 12 }}
            tickFormatter={(date: string) => formatDate(date).replace(/\s?\d{4}$/, "")}
          />
          <YAxis
            hide={bare}
            width={72}
            tickLine={false}
            axisLine={false}
            domain={["auto", "auto"]}
            tick={{ fill: "var(--text-muted)", fontSize: 12 }}
            tickFormatter={(value: number) => formatMoney(value, "EUR", { compact: true })}
          />
          <Tooltip
            cursor={{ stroke: "var(--text-muted)", strokeWidth: 1 }}
            content={({ active, payload }) =>
              active && payload?.[0] ? (
                <ChartTooltip
                  title={formatDate(String(payload[0].payload.date))}
                  rows={[{ label, value: formatMoney(Number(payload[0].value)) }]}
                />
              ) : null
            }
          />
          <Area
            type="monotone"
            dataKey="balance"
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