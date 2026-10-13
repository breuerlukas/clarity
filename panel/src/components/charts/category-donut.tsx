import { useTranslation } from "react-i18next";
import { Pie, PieChart, ResponsiveContainer, Tooltip } from "recharts";
import { ChartTooltip, Swatch } from "@/components/charts/chart-tooltip";
import type { Category } from "@/lib/api";
import { formatMoney, formatPercent } from "@/lib/format";
import type { CategoryRow } from "@/lib/statistics";

const SLOTS = 5;

interface Slice {
  category: Category | null;
  label: string;
  amount: number;
  fill: string;
}

/**
 * How the spending splits up: the five largest categories, everything else
 * folded into one gray slice so the ring stays readable. Rows come largest
 * first, so the colors follow the rank within the chosen period.
 */
export function CategoryDonut({
  rows,
  onSelect,
}: {
  rows: CategoryRow[];
  onSelect: (category: Category) => void;
}) {
  const { t } = useTranslation();
  const total = rows.reduce((sum, row) => sum + row.total, 0);
  const slices: Slice[] = rows.slice(0, SLOTS).map((row, index) => ({
    category: row.category,
    label: t(`category.${row.category}`),
    amount: row.total,
    fill: `var(--series-${index + 1})`,
  }));
  const rest = rows.slice(SLOTS).reduce((sum, row) => sum + row.total, 0);
  if (rest > 0) {
    slices.push({ category: null, label: t("statistics.share_rest"), amount: rest, fill: "var(--series-rest)" });
  }
  const animate = !window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  return (
    <div className="grid items-center gap-5 sm:grid-cols-[168px_minmax(0,1fr)]">
      <div className="relative mx-auto size-[168px]">
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie
              data={slices}
              dataKey="amount"
              nameKey="label"
              innerRadius="64%"
              outerRadius="100%"
              startAngle={90}
              endAngle={-270}
              stroke="var(--card)"
              strokeWidth={2}
              isAnimationActive={animate}
              animationDuration={700}
              animationEasing="ease-out"
              className="cursor-pointer outline-none"
              onClick={(sector) => {
                const { category } = sector.payload as Slice;
                if (category) {
                  onSelect(category);
                }
              }}
            />
            <Tooltip
              wrapperStyle={{ zIndex: 10 }}
              content={({ active, payload }) => {
                const slice = payload?.[0]?.payload as Slice | undefined;
                return active && slice ? (
                  <ChartTooltip
                    title={slice.label}
                    rows={[{ label: formatPercent(slice.amount / total), value: formatMoney(slice.amount), swatch: slice.fill }]}
                  />
                ) : null;
              }}
            />
          </PieChart>
        </ResponsiveContainer>
        <span className="pointer-events-none absolute inset-0 grid place-content-center text-center">
          <span className="text-xs text-muted-foreground">{t("statistics.money_out")}</span>
          <span className="figures text-base font-extrabold">{formatMoney(total, "EUR", { compact: true })}</span>
        </span>
      </div>
      <ul className="grid gap-1.5 text-sm">
        {slices.map((slice) => (
          <li key={slice.label} className="flex items-center justify-between gap-3">
            <span className="flex min-w-0 items-center gap-2">
              <Swatch color={slice.fill} />
              <span className="truncate">{slice.label}</span>
            </span>
            <span className="figures font-bold text-muted-foreground">{formatPercent(slice.amount / total)}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}
