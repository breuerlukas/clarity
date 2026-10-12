import { useState } from "react";
import { useTranslation } from "react-i18next";
import { PageHeader, Panel } from "@/components/panel";
import { Notice } from "@/components/notice";
import { BalanceChart } from "@/components/charts/balance-chart";
import { CashflowChart } from "@/components/charts/cashflow-chart";
import { useConnections, useHistory, useRecurringPayments } from "@/hooks/use-finances";
import { total } from "@/lib/overview";
import { balanceHistory, lastMonths, monthlyStatistics } from "@/lib/statistics";
import { Segmented } from "@/components/segmented";
import { KeyFigures } from "@/pages/statistics/key-figures";
import { CategoryTable } from "@/pages/statistics/category-table";
import { TopCounterparties } from "@/pages/statistics/top-counterparties";
import { LargestExpenses } from "@/pages/statistics/largest-expenses";

const RANGES = [3, 6, 12] as const;

export function StatisticsPage() {
  const { t } = useTranslation();
  const [range, setRange] = useState<(typeof RANGES)[number]>(6);
  const history = useHistory();
  const connections = useConnections();
  const recurring = useRecurringPayments();
  const months = lastMonths(range);
  const inRange = (history.data ?? []).filter(
    (transaction) => transaction.date >= `${months[0]}-01`,
  );
  const statistics = monthlyStatistics(inRange, months);
  const balance = total(
    (connections.data ?? []).flatMap((connection) =>
      connection.accounts.map((account) => account.balance ?? 0),
    ),
  );
  const curve = balanceHistory(history.data ?? [], balance).filter(
    (point) => point.date >= `${months[0]}-01`,
  );
  return (
    <>
      <PageHeader title={t("nav.statistics")}>
        <Segmented
          label={t("statistics.range")}
          value={String(range)}
          options={RANGES.map((option) => ({
            value: String(option),
            label: t("statistics.months", { count: option }),
          }))}
          onChange={(value) => setRange(Number(value) as (typeof RANGES)[number])}
        />
      </PageHeader>
      {history.isError && <Notice className="mb-6">{t("common.error")}</Notice>}
      {history.data ? (
        <div className="grid gap-6">
          <KeyFigures statistics={statistics} payments={recurring.data} />
          <div className="grid gap-6 xl:grid-cols-2">
            <Panel title={t("statistics.cashflow")}>
              <CashflowChart statistics={statistics} height={260} />
            </Panel>
            <Panel title={t("statistics.balance")}>
              {curve.length > 1 ? (
                <BalanceChart data={curve} height={288} label={t("statistics.balance")} />
              ) : (
                <p className="text-muted-foreground">{t("statistics.not_enough")}</p>
              )}
            </Panel>
          </div>
          <CategoryTable transactions={inRange} months={months} />
          <div className="grid gap-6 lg:grid-cols-2">
            <TopCounterparties transactions={inRange} />
            <LargestExpenses transactions={inRange} payments={recurring.data} />
          </div>
        </div>
      ) : (
        <div className="h-96" aria-busy="true" />
      )}
    </>
  );
}