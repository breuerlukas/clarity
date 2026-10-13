import { useTranslation } from "react-i18next";
import { PageHeader, PanelSkeleton } from "@/components/panel";
import { Notice } from "@/components/notice";
import { useRecurringPayments } from "@/hooks/use-finances";
import type { Category } from "@/lib/api";
import { formatMoney } from "@/lib/format";
import { total, yearly } from "@/lib/overview";
import { FixedCostFigures, FixedCostsList } from "@/pages/subscriptions/fixed-costs-list";

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

/** Subscriptions and other fixed costs, grouped, with what they cost a year. */
export function SubscriptionsPage() {
  const { t } = useTranslation();
  const recurring = useRecurringPayments();
  const groups = GROUPS.map((group) => ({
    key: group.key,
    payments: (recurring.data ?? []).filter((payment) =>
      group.categories ? group.categories.includes(payment.category) : !NAMED.has(payment.category),
    ),
  })).filter((group) => group.payments.length > 0);
  return (
    <>
      <PageHeader title={t("nav.subscriptions")} />
      {recurring.isError && <Notice className="mb-6">{t("common.error")}</Notice>}
      {recurring.data ? (
        <div className="grid gap-6">
          <FixedCostFigures payments={recurring.data} />
          <p className="-mt-2 text-sm text-muted-foreground">{t("statistics.fixed_note")}</p>
          {groups.length === 0 && <FixedCostsList payments={[]} title={t("nav.subscriptions")} />}
          <div className="grid items-start gap-6 xl:grid-cols-2">
            {groups.map((group) => (
              <FixedCostsList
                key={group.key}
                payments={group.payments}
                title={t(`subscriptions.group_${group.key}`)}
                aside={t("subscriptions.per_month", {
                  amount: formatMoney(total(group.payments.map(yearly)) / 12),
                })}
              />
            ))}
          </div>
        </div>
      ) : (
        <PanelSkeleton className="h-96" />
      )}
    </>
  );
}

