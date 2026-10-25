import { useState } from "react";
import { useTranslation } from "react-i18next";
import { PageHeader, PanelSkeleton } from "@/components/panel";
import { Notice } from "@/components/notice";
import { useAllRecurringPayments } from "@/hooks/use-finances";
import type { Category, RecurringPayment } from "@/lib/api";
import { formatMoney } from "@/lib/format";
import { total, yearly } from "@/lib/overview";
import { FixedCostFigures, FixedCostsList } from "@/pages/subscriptions/fixed-costs-list";
import { SubscriptionDialog } from "@/pages/subscriptions/subscription-dialog";

/**
 * Groups by what the money is for, so the page reads like a budget instead
 * of one long mixed list. Anything not named here lands in "other".
 */
const GROUPS: { key: string; categories?: Category[] }[] = [
  { key: "home", categories: ["HOUSING", "UTILITIES"] },
  { key: "subscriptions", categories: ["SUBSCRIPTIONS"] },
  { key: "insurance", categories: ["INSURANCE"] },
  { key: "other" },
  { key: "savings", categories: ["SAVINGS"] },
];

const NAMED = new Set(GROUPS.flatMap((group) => group.categories ?? []));

/**
 * Subscriptions and other fixed costs, grouped, with what they cost a year.
 * Ended ones leave the totals and wait in their own list until they drop
 * out of the history. Ones the owner said are none don't show at all.
 */
export function SubscriptionsPage() {
  const { t } = useTranslation();
  const recurring = useAllRecurringPayments();
  const [selected, setSelected] = useState<RecurringPayment | null>(null);
  const counted = recurring.data?.filter((payment) => !payment.ignored) ?? [];
  const running = counted.filter((payment) => payment.status !== "ENDED");
  const ended = counted.filter((payment) => payment.status === "ENDED");
  const groups = GROUPS.map((group) => ({
    key: group.key,
    payments: running.filter((payment) =>
      group.categories ? group.categories.includes(payment.category) : !NAMED.has(payment.category),
    ),
  })).filter((group) => group.payments.length > 0);
  return (
    <>
      <PageHeader title={t("nav.subscriptions")} />
      {recurring.isError && <Notice className="mb-6">{t("common.error")}</Notice>}
      {recurring.data ? (
        <div className="grid gap-6">
          <FixedCostFigures payments={running} />
          <p className="-mt-2 text-sm text-muted-foreground">{t("statistics.fixed_note")}</p>
          {groups.length === 0 && <FixedCostsList payments={[]} title={t("nav.subscriptions")} onSelect={setSelected} />}
          <div className="grid items-start gap-6 xl:grid-cols-2">
            {groups.map((group) => (
              <FixedCostsList
                key={group.key}
                payments={group.payments}
                onSelect={setSelected}
                title={t(`subscriptions.group_${group.key}`)}
                aside={t("subscriptions.per_month", {
                  amount: formatMoney(total(group.payments.map(yearly)) / 12),
                })}
              />
            ))}
            {ended.length > 0 && (
              <FixedCostsList payments={ended} title={t("subscriptions.group_ended")} onSelect={setSelected} />
            )}
          </div>
        </div>
      ) : (
        <PanelSkeleton className="h-96" />
      )}
      <SubscriptionDialog payment={selected} onClose={() => setSelected(null)} />
    </>
  );
}

