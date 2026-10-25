import { useTranslation } from "react-i18next";
import { Panel } from "@/components/panel";
import { BalanceChart } from "@/components/charts/balance-chart";
import { CashflowChart, CashflowLegend } from "@/components/charts/cashflow-chart";
import type { Connection, RecurringPayment, Transaction } from "@/lib/api";
import type { MonthKey } from "@/lib/dates";
import { total } from "@/lib/overview";
import { balanceHistory, monthlyStatistics } from "@/lib/statistics";
import { KeyFigures } from "@/pages/statistics/key-figures";

/** Averages, money in and out per month, and the balance over the period. */
export function OverviewTab({
  transactions,
  untilToday,
  months,
  connections,
  payments,
  from,
  to,
}: {
  transactions: Transaction[];
  untilToday: Transaction[];
  months: MonthKey[];
  connections?: Connection[];
  payments?: RecurringPayment[];
  from: string;
  to: string;
}) {
  const { t } = useTranslation();
  const statistics = monthlyStatistics(transactions, months);
  const balance = total(
    (connections ?? []).flatMap((connection) => connection.accounts.map((account) => account.balance ?? 0)),
  );
  const curve = balanceHistory(untilToday, balance).filter(
    (point) => point.date >= from && point.date <= to,
  );
  return (
    <div className="grid gap-6">
      <KeyFigures statistics={statistics} payments={payments} />
      <div className="grid gap-6 xl:grid-cols-2">
        <Panel title={t("statistics.cashflow")} aside={<CashflowLegend />}>
          <CashflowChart statistics={statistics} height={300} legend={false} />
        </Panel>
        <Panel title={t("statistics.balance")}>
          {curve.length > 1 ? (
            <BalanceChart data={curve} height={300} label={t("statistics.balance")} />
          ) : (
            <p className="text-muted-foreground">{t("statistics.not_enough")}</p>
          )}
        </Panel>
      </div>
    </div>
  );
}
