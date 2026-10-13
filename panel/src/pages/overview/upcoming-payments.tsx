import { useTranslation } from "react-i18next";
import { Panel } from "@/components/panel";
import { AmountBadge } from "@/components/amount-badge";
import { CategoryIcon } from "@/components/category-icon";
import { useSpacePath } from "@/hooks/use-space";
import type { RecurringPayment } from "@/lib/api";
import { formatDay } from "@/lib/format";
import { cn } from "@/lib/utils";

const VISIBLE = 6;

/** The next fixed costs in date order; the ones before payday stand out. */
export function UpcomingPayments({
  payments,
  payday,
  className,
}: {
  payments?: RecurringPayment[];
  payday?: string;
  className?: string;
}) {
  const { t } = useTranslation();
  const path = useSpacePath();
  const next = [...(payments ?? [])]
    .sort((first, second) => first.next_date.localeCompare(second.next_date))
    .slice(0, VISIBLE);
  return (
    <Panel title={t("overview.upcoming")} viewAll={path("/subscriptions")} className={className}>
      {payments?.length === 0 && <p className="text-muted-foreground">{t("overview.fixed_empty")}</p>}
      <ul className="zebra -mx-2">
        {next.map((payment) => (
          <li
            key={`${payment.counterparty}-${payment.cycle}`}
            className="zebra-row grid grid-cols-[auto_1fr_auto] items-center gap-3 rounded-xl px-2 py-2"
          >
            <CategoryIcon category={payment.category} />
            <span className="grid min-w-0 gap-0.5">
              <span className="truncate font-medium">{payment.counterparty}</span>
              <span className="flex flex-wrap items-center gap-1.5 text-sm text-muted-foreground">
                <span
                  className={cn(
                    "rounded-full px-2 py-px text-xs font-bold",
                    payday && payment.next_date < payday
                      ? "bg-warning/12 text-warning"
                      : "bg-raised text-muted-foreground ring-1 ring-divider",
                  )}
                >
                  {formatDay(payment.next_date)}
                </span>
                {t(`cycle.${payment.cycle}`)}
              </span>
            </span>
            <AmountBadge value={-payment.amount} currency={payment.currency} />
          </li>
        ))}
      </ul>
    </Panel>
  );
}
