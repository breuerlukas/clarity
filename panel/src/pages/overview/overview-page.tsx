import { Link } from "react-router";
import { useTranslation } from "react-i18next";
import { Button } from "@/components/ui/button";
import { Notice } from "@/components/notice";
import { PageHeader, PanelSkeleton } from "@/components/panel";
import { SyncButton } from "@/components/sync-button";
import { useCashWallet } from "@/hooks/use-cash";
import { useBudgets } from "@/hooks/use-categories";
import { useConnections, useHistory, useRecurringPayments } from "@/hooks/use-finances";
import { useSpacePath } from "@/hooks/use-space";
import { budgetUsage, cashAccount, spendingRoom } from "@/lib/budget";
import { SpendingRoomCard } from "@/pages/overview/spending-room-card";
import { BudgetsCard } from "@/pages/overview/budgets-card";
import { AccountsCard } from "@/pages/overview/accounts-card";
import { ActivityCard } from "@/pages/overview/activity-card";
import { BalanceCard } from "@/pages/overview/balance-card";
import { UpcomingPayments } from "@/pages/overview/upcoming-payments";
import { RecentTransactions } from "@/pages/overview/recent-transactions";

/**
 * Left: what is free until payday, how the budgets fare, how the balance
 * moved, what just happened. Right: the accounts and what is due next.
 */
export function OverviewPage() {
  const { t } = useTranslation();
  const connections = useConnections();
  const recurring = useRecurringPayments();
  const history = useHistory();
  const cash = useCashWallet();
  const budgets = useBudgets();
  const path = useSpacePath();
  if (connections.data?.length === 0) {
    return <EmptyOverview />;
  }
  const accounts = connections.data?.flatMap((connection) => connection.accounts);
  const room =
    accounts && recurring.data && history.data && cash.data
      ? spendingRoom([...accounts, cashAccount(cash.data, t("cash.title"))], recurring.data, history.data)
      : null;
  const usage = room && history.data && budgets.data?.length ? budgetUsage(budgets.data, history.data, room.cycleStart) : null;
  return (
    <>
      <PageHeader title={t("nav.overview")}>
        <SyncButton connections={connections.data} />
      </PageHeader>
      {(connections.isError || recurring.isError || history.isError || cash.isError) && (
        <Notice className="mb-6">{t("common.error")}</Notice>
      )}
      <div className="grid gap-6 xl:grid-cols-[minmax(0,1.6fr)_minmax(0,1fr)]">
        <div className="flex min-w-0 flex-col gap-6">
          {room ? <SpendingRoomCard room={room} /> : <PanelSkeleton className="h-72" />}
          {usage && <BudgetsCard usage={usage} />}
          {accounts && history.data ? (
            <BalanceCard accounts={accounts} history={history.data} />
          ) : (
            <PanelSkeleton className="h-80" />
          )}
          <RecentTransactions transactions={history.data} className="flex-1" />
        </div>
        <div className="flex min-w-0 flex-col gap-6">
          <AccountsCard connections={connections.data} cash={cash.data} viewAll={path("/accounts")} />
          <ActivityCard transactions={history.data} />
          <UpcomingPayments payments={recurring.data} payday={room?.payday} className="flex-1" />
        </div>
      </div>
    </>
  );
}

function EmptyOverview() {
  const { t } = useTranslation();
  const path = useSpacePath();
  return (
    <div className="grid max-w-xl gap-4 pt-10">
      <h1 className="text-[30px] leading-tight font-extrabold tracking-[-0.02em]">
        {t("overview.empty_title")}
      </h1>
      <p className="text-muted-foreground">{t("overview.empty_text")}</p>
      <Button asChild size="lg" className="mt-2 justify-self-start">
        <Link to={path("/accounts")}>{t("accounts.connect")}</Link>
      </Button>
    </div>
  );
}
