import { useState } from "react";
import { useTranslation } from "react-i18next";
import { PageHeader } from "@/components/panel";
import { Segmented } from "@/components/segmented";
import { Notice } from "@/components/notice";
import { useRecurringPayments } from "@/hooks/use-finances";
import type { RecurringPayment } from "@/lib/api";
import { FixedCostFigures, FixedCostsList } from "@/pages/subscriptions/fixed-costs-list";

type Filter = "all" | "subscriptions" | "contracts";

const matches: Record<Filter, (payment: RecurringPayment) => boolean> = {
  all: () => true,
  subscriptions: (payment) => payment.category === "SUBSCRIPTIONS",
  contracts: (payment) => payment.category !== "SUBSCRIPTIONS",
};

/** Subscriptions and other fixed costs, with what they cost a year. */
export function SubscriptionsPage() {
  const { t } = useTranslation();
  const recurring = useRecurringPayments();
  const [filter, setFilter] = useState<Filter>("all");
  return (
    <>
      <PageHeader title={t("nav.subscriptions")}>
        <Segmented<Filter>
          label={t("subscriptions.filter")}
          value={filter}
          options={(["all", "subscriptions", "contracts"] as const).map((value) => ({
            value,
            label: t(`subscriptions.${value}`),
          }))}
          onChange={setFilter}
        />
      </PageHeader>
      {recurring.isError && <Notice className="mb-6">{t("common.error")}</Notice>}
      {recurring.data ? (
        <div className="grid gap-6">
          <FixedCostFigures payments={recurring.data} />
          <FixedCostsList
            payments={recurring.data.filter(matches[filter])}
            title={t(`subscriptions.${filter}`)}
          />
        </div>
      ) : (
        <div className="h-96 rounded-3xl border bg-card" aria-busy="true" />
      )}
    </>
  );
}
