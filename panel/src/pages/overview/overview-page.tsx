import { Link } from "react-router";
import { useTranslation } from "react-i18next";
import { Button } from "@/components/ui/button";
import { Notice } from "@/components/notice";
import { PageHeader } from "@/components/panel";
import { SyncButton } from "@/components/sync-button";
import {
  useConnections,
  useHistory,
  useMonthTransactions,
  useRecurringPayments,
} from "@/hooks/use-finances";
import { useMonthParam } from "@/hooks/use-month-param";
import { BalanceHero } from "@/pages/overview/balance-hero";
import { MonthPanel } from "@/pages/overview/month-panel";
import { FixedCosts } from "@/pages/overview/fixed-costs";
import { RecentTransactions } from "@/pages/overview/recent-transactions";
import { CashflowTeaser } from "@/pages/overview/cashflow-teaser";

export function OverviewPage() {
  const { t } = useTranslation();
  const [month, setMonth] = useMonthParam();
  const connections = useConnections();
  const transactions = useMonthTransactions(month);
  const recurring = useRecurringPayments();
  const history = useHistory();
  if (connections.data?.length === 0) {
    return <EmptyOverview />;
  }
  const accounts = connections.data?.flatMap((connection) => connection.accounts);
  const failed =
    connections.isError || transactions.isError || recurring.isError;
  return (
    <>
      <PageHeader title={t("nav.overview")}>
        <SyncButton />
      </PageHeader>
      {failed && <Notice className="mb-6">{t("common.error")}</Notice>}
      <div className="grid items-start gap-6 xl:grid-cols-[minmax(0,1.7fr)_minmax(0,1fr)]">
        <div className="grid min-w-0 gap-6">
          <BalanceHero accounts={accounts} history={history.data} />
          <div className="grid items-start gap-6 2xl:grid-cols-2">
            <MonthPanel month={month} onMonthChange={setMonth} transactions={transactions.data} />
            <RecentTransactions month={month} transactions={transactions.data} />
          </div>
        </div>
        <div className="grid min-w-0 gap-6">
          <FixedCosts payments={recurring.data} accounts={accounts} />
          <CashflowTeaser history={history.data} />
        </div>
      </div>
    </>
  );
}

function EmptyOverview() {
  const { t } = useTranslation();
  return (
    <div className="grid max-w-xl gap-4 pt-10">
      <h1 className="text-[30px] leading-tight font-extrabold tracking-[-0.02em]">
        {t("overview.empty_title")}
      </h1>
      <p className="text-muted-foreground">{t("overview.empty_text")}</p>
      <Button asChild size="lg" className="mt-2 justify-self-start">
        <Link to="/accounts">{t("accounts.connect")}</Link>
      </Button>
    </div>
  );
}