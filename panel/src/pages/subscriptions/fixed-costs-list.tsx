import { useTranslation } from "react-i18next";
import { TrendingDown, TrendingUp } from "lucide-react";
import { Panel } from "@/components/panel";
import { AmountBadge } from "@/components/amount-badge";
import { CategoryIcon } from "@/components/category-icon";
import type { RecurringPayment } from "@/lib/api";
import { formatDate, formatMoney } from "@/lib/format";
import { total, yearly } from "@/lib/overview";

/**
 * Yearly and monthly totals, subscriptions alone, and price changes. Savings
 * plans are money put aside, not a cost, so they stay out of the totals.
 */
export function FixedCostFigures({ payments }: { payments: RecurringPayment[] }) {
  const { t } = useTranslation();
  const costs = payments.filter((payment) => payment.category !== "SAVINGS");
  const perYear = total(costs.map(yearly));
  const subscriptions = payments.filter((payment) => payment.category === "SUBSCRIPTIONS");
  const changed = payments.filter((payment) => Math.abs(payment.amount - payment.previous_amount) >= 0.01);
  const figures = [
    { label: t("statistics.fixed_yearly"), value: formatMoney(perYear) },
    { label: t("statistics.fixed_monthly"), value: formatMoney(perYear / 12) },
    { label: t("statistics.subscriptions_yearly"), value: formatMoney(total(subscriptions.map(yearly))) },
    { label: t("statistics.price_changes"), value: String(changed.length) },
  ];
  return (
    <section className="overflow-hidden rounded-3xl border bg-card">
      <dl className="grid grid-cols-2 gap-px bg-divider lg:grid-cols-4">
        {figures.map((figure) => (
          <div key={figure.label} className="grid content-start gap-1 bg-card p-5 sm:p-6">
            <dt className="text-sm text-muted-foreground">{figure.label}</dt>
            <dd className="figures text-[clamp(1.25rem,4.5vw,1.75rem)] leading-tight font-extrabold">{figure.value}</dd>
          </div>
        ))}
      </dl>
    </section>
  );
}

/**
 * One group of what leaves the account on its own, largest first, with what
 * each costs a year and which got more expensive lately.
 */
export function FixedCostsList({
  payments,
  title,
  aside,
}: {
  payments: RecurringPayment[];
  title: string;
  aside?: string;
}) {
  const { t } = useTranslation();
  return (
    <Panel title={title} aside={aside}>
      {payments.length === 0 && <p className="text-muted-foreground">{t("overview.fixed_empty")}</p>}
      <ul className="zebra -mx-2">
        {[...payments].sort((first, second) => yearly(second) - yearly(first)).map((payment) => (
          <li
            key={`${payment.counterparty}-${payment.cycle}`}
            className="zebra-row grid grid-cols-[auto_minmax(0,1fr)_auto] items-center gap-3 rounded-xl px-2 py-2.5"
          >
            <CategoryIcon category={payment.category} />
            <span className="grid min-w-0 gap-1">
              <span className="flex min-w-0 flex-wrap items-center gap-x-2 gap-y-1">
                <span className="max-w-full truncate font-medium">{payment.counterparty}</span>
                <PriceChange payment={payment} />
              </span>
              <span className="text-sm text-muted-foreground">
                {t(`cycle.${payment.cycle}`)}, {t("overview.fixed_next", { date: formatDate(payment.next_date) })}
              </span>
            </span>
            <span className="grid justify-items-end gap-1">
              <AmountBadge value={-payment.amount} currency={payment.currency} />
              <span className="text-xs whitespace-nowrap text-muted-foreground">
                {t("statistics.per_year", { amount: formatMoney(yearly(payment)) })}
              </span>
            </span>
          </li>
        ))}
      </ul>
    </Panel>
  );
}

function PriceChange({ payment }: { payment: RecurringPayment }) {
  const { t } = useTranslation();
  const difference = payment.amount - payment.previous_amount;
  if (Math.abs(difference) < 0.01) {
    return null;
  }
  const higher = difference > 0;
  return (
    <span
      className={
        higher
          ? "inline-flex items-center gap-1 rounded-full bg-warning/12 px-2 py-px text-xs font-bold text-warning"
          : "inline-flex items-center gap-1 rounded-full bg-positive/12 px-2 py-px text-xs font-bold text-positive"
      }
    >
      {higher ? <TrendingUp className="size-3.5" aria-hidden /> : <TrendingDown className="size-3.5" aria-hidden />}
      {t(higher ? "statistics.price_up" : "statistics.price_down", {
        amount: formatMoney(Math.abs(difference), payment.currency),
      })}
    </span>
  );
}
