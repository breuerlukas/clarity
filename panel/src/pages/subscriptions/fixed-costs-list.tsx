import { useTranslation } from "react-i18next";
import { TrendingDown, TrendingUp } from "lucide-react";
import { Panel } from "@/components/panel";
import { AmountBadge } from "@/components/amount-badge";
import { CategoryIcon } from "@/components/category-icon";
import type { RecurringPayment } from "@/lib/api";
import { formatDate, formatMoney } from "@/lib/format";
import { total } from "@/lib/overview";

/** Yearly and monthly totals, subscriptions alone, and price changes. */
export function FixedCostFigures({ payments }: { payments: RecurringPayment[] }) {
  const { t } = useTranslation();
  const monthly = total(payments.map((payment) => payment.monthly_amount));
  const subscriptions = payments.filter((payment) => payment.category === "SUBSCRIPTIONS");
  const changed = payments.filter((payment) => Math.abs(payment.amount - payment.previous_amount) >= 0.01);
  const figures = [
    { label: t("statistics.fixed_yearly"), value: formatMoney(monthly * 12) },
    { label: t("statistics.fixed_monthly"), value: formatMoney(monthly) },
    { label: t("statistics.subscriptions_yearly"), value: formatMoney(total(subscriptions.map((payment) => payment.monthly_amount)) * 12) },
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
 * Everything that leaves the account on its own, what it costs a year, and
 * which of it got more expensive lately.
 */
export function FixedCostsList({
  payments,
  title,
}: {
  payments: RecurringPayment[];
  title: string;
}) {
  const { t } = useTranslation();
  return (
      <Panel title={title} aside={t("statistics.fixed_note")}>
        {payments.length === 0 && <p className="text-muted-foreground">{t("overview.fixed_empty")}</p>}
        <ul className="zebra -mx-2">
          {payments.map((payment) => (
            <li
              key={`${payment.counterparty}-${payment.cycle}`}
              className="zebra-row grid grid-cols-[auto_1fr_auto] items-center gap-3 rounded-xl px-2 py-2.5 sm:grid-cols-[auto_1fr_auto_auto]"
            >
              <CategoryIcon category={payment.category} />
              <span className="grid min-w-0 gap-1">
                <span className="flex flex-wrap items-center gap-2">
                  <span className="truncate font-medium">{payment.counterparty}</span>
                  <PriceChange payment={payment} />
                </span>
                <span className="text-sm text-muted-foreground">
                  {t(`cycle.${payment.cycle}`)}, {t("overview.fixed_next", { date: formatDate(payment.next_date) })}
                </span>
              </span>
              <span className="hidden text-right text-sm text-muted-foreground sm:block">
                {t("statistics.per_year", { amount: formatMoney(payment.monthly_amount * 12) })}
              </span>
              <AmountBadge value={-payment.amount} currency={payment.currency} />
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
