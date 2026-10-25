import { useTranslation } from "react-i18next";
import { BellRing, CircleAlert, CircleOff, TrendingDown, TrendingUp } from "lucide-react";
import { Panel } from "@/components/panel";
import { AmountBadge } from "@/components/amount-badge";
import { CategoryIcon } from "@/components/category-icon";
import type { RecurringPayment } from "@/lib/api";
import { formatDate, formatMoney } from "@/lib/format";
import { paymentName, total, yearly } from "@/lib/overview";

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
 * each costs a year, which got more expensive lately and which stopped.
 * Choosing one opens what the owner can tell about it.
 */
export function FixedCostsList({
  payments,
  title,
  aside,
  onSelect,
}: {
  payments: RecurringPayment[];
  title: string;
  aside?: string;
  onSelect: (payment: RecurringPayment) => void;
}) {
  const { t } = useTranslation();
  return (
    <Panel title={title} aside={aside}>
      {payments.length === 0 && <p className="text-muted-foreground">{t("overview.fixed_empty")}</p>}
      <ul className="zebra -mx-2">
        {[...payments].sort((first, second) => yearly(second) - yearly(first)).map((payment) => (
          <li key={payment.key} className="zebra-row rounded-xl">
            <button
              type="button"
              onClick={() => onSelect(payment)}
              className="grid w-full grid-cols-[auto_minmax(0,1fr)_auto] items-center gap-3 rounded-xl px-2 py-2.5 text-left transition-colors hover:bg-raised"
            >
              <CategoryIcon category={payment.category} />
              <span className="grid min-w-0 gap-1">
                <span className="flex min-w-0 flex-wrap items-center gap-x-2 gap-y-1">
                  <span className="max-w-full truncate font-medium">{paymentName(payment)}</span>
                  <PriceChange payment={payment} />
                  <StatusBadge payment={payment} />
                </span>
                <span className="text-sm text-muted-foreground">
                  {t(`cycle.${payment.cycle}`)},{" "}
                  {payment.status === "ENDED"
                    ? t("subscriptions.last_paid", { date: formatDate(payment.last_date) })
                    : t("overview.fixed_next", { date: formatDate(payment.next_date) })}
                </span>
              </span>
              <span className="grid justify-items-end gap-1">
                <AmountBadge value={-payment.amount} currency={payment.currency} />
                <span className="text-xs whitespace-nowrap text-muted-foreground">
                  {t("statistics.per_year", { amount: formatMoney(yearly(payment)) })}
                </span>
              </span>
            </button>
          </li>
        ))}
      </ul>
    </Panel>
  );
}

const BADGE = "inline-flex items-center gap-1 rounded-full px-2 py-px text-xs font-bold";

/** Missing, cancelled or ended, and when to cancel with a notice period. */
function StatusBadge({ payment }: { payment: RecurringPayment }) {
  const { t } = useTranslation();
  if (payment.status === "ENDED") {
    return (
      <span className={`${BADGE} bg-raised text-muted-foreground ring-1 ring-divider`}>
        <CircleOff className="size-3.5" aria-hidden />
        {t(payment.cancelled_on ? "subscriptions.cancelled" : "subscriptions.status_ENDED")}
      </span>
    );
  }
  if (payment.cancelled_on) {
    return (
      <span className={`${BADGE} bg-raised text-muted-foreground ring-1 ring-divider`}>
        <CircleOff className="size-3.5" aria-hidden />
        {t("subscriptions.ends_on", { date: formatDate(payment.cancelled_on) })}
      </span>
    );
  }
  if (payment.status === "MISSING") {
    return (
      <span className={`${BADGE} bg-warning/12 text-warning`}>
        <CircleAlert className="size-3.5" aria-hidden />
        {t("subscriptions.status_MISSING")}
      </span>
    );
  }
  return payment.cancel_by ? (
    <span className={`${BADGE} bg-raised text-muted-foreground ring-1 ring-divider`}>
      <BellRing className="size-3.5" aria-hidden />
      {t("subscriptions.cancel_by", { date: formatDate(payment.cancel_by) })}
    </span>
  ) : null;
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
