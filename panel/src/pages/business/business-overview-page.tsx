import { Link } from "react-router";
import { useTranslation } from "react-i18next";
import { Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Notice } from "@/components/notice";
import { PageHeader, Panel } from "@/components/panel";
import { RevenueChart } from "@/components/charts/revenue-chart";
import { useBusinessConnections, useContracts } from "@/hooks/use-business";
import { HISTORY_MONTHS } from "@/hooks/use-finances";
import { revenueHistory, upcomingEvents, type RevenuePoint } from "@/lib/business";
import { isoDate } from "@/lib/dates";
import { formatMoney, formatMonth, formatPercent } from "@/lib/format";
import { lastMonths } from "@/lib/statistics";
import { AccountsCard } from "@/pages/overview/accounts-card";
import { ContractEvents } from "@/pages/business/contract-events";

/**
 * The commercial state at a glance: recurring revenue, customers and churn
 * from the contracts, how revenue developed over a year, what comes up next
 * and what is on the business accounts.
 */
export function BusinessOverviewPage() {
  const { t } = useTranslation();
  const contracts = useContracts();
  const connections = useBusinessConnections();
  if (contracts.data?.length === 0) {
    return <EmptyBusiness />;
  }
  const history = contracts.data ? revenueHistory(contracts.data, lastMonths(HISTORY_MONTHS)) : null;
  const labels = {
    mrr: t("business.mrr"),
    arr: t("business.arr"),
    customers: t("business.customers"),
  };
  return (
    <>
      <PageHeader title={t("nav.business_overview")} />
      {(contracts.isError || connections.isError) && <Notice className="mb-6">{t("common.error")}</Notice>}
      {history && contracts.data ? (
        <div className="grid gap-6">
          <RevenueFigures history={history} />
          <div className="grid gap-6 xl:grid-cols-[minmax(0,1.6fr)_minmax(0,1fr)]">
            <div className="flex min-w-0 flex-col gap-6">
              <Panel title={t("business.revenue_trend")} aside={t("business.revenue_note")}>
                <RevenueChart data={history} height={300} labels={labels} />
              </Panel>
              <ContractEvents events={upcomingEvents(contracts.data, isoDate(new Date()))} className="flex-1" />
            </div>
            <div className="flex min-w-0 flex-col gap-6">
              {connections.data?.length ? (
                <AccountsCard connections={connections.data} viewAll="/business/accounts" business />
              ) : (
                <Panel title={t("nav.accounts")}>
                  <p className="text-muted-foreground">{t("business.accounts_empty")}</p>
                  <Button asChild variant="outline" className="self-start">
                    <Link to="/business/accounts">{t("accounts.connect")}</Link>
                  </Button>
                </Panel>
              )}
            </div>
          </div>
        </div>
      ) : (
        <div className="h-96 rounded-3xl border bg-card" aria-busy="true" />
      )}
    </>
  );
}

/** MRR with the change to last month, ARR, customers and last month's churn. */
function RevenueFigures({ history }: { history: RevenuePoint[] }) {
  const { t } = useTranslation();
  const current = history[history.length - 1];
  const previous = history[history.length - 2];
  const change = previous ? current.mrr - previous.mrr : 0;
  const figures = [
    {
      label: t("business.mrr"),
      value: formatMoney(current.mrr),
      note: previous ? t("business.mrr_change", { amount: formatMoney(change, "EUR", { signed: true }) }) : null,
    },
    { label: t("business.arr"), value: formatMoney(current.arr), note: null },
    { label: t("business.customers"), value: String(current.customers), note: null },
    {
      label: t("business.churn"),
      value: previous?.churn == null ? "–" : formatPercent(previous.churn),
      note: previous ? formatMonth(previous.month) : null,
    },
  ];
  return (
    <section className="overflow-hidden rounded-3xl border bg-card">
      <dl className="grid grid-cols-2 gap-px bg-divider lg:grid-cols-4">
        {figures.map((figure) => (
          <div key={figure.label} className="grid content-start gap-1 bg-card p-5 sm:p-6">
            <dt className="text-sm text-muted-foreground">{figure.label}</dt>
            <dd className="figures text-[clamp(1.25rem,4.5vw,1.75rem)] leading-tight font-extrabold">{figure.value}</dd>
            {figure.note && <dd className="text-sm text-muted-foreground">{figure.note}</dd>}
          </div>
        ))}
      </dl>
    </section>
  );
}

function EmptyBusiness() {
  const { t } = useTranslation();
  return (
    <div className="grid max-w-xl gap-4 pt-10">
      <h1 className="text-[30px] leading-tight font-extrabold tracking-[-0.02em]">
        {t("business.empty_title")}
      </h1>
      <p className="text-muted-foreground">{t("business.empty_text")}</p>
      <Button asChild size="lg" className="mt-2 justify-self-start">
        <Link to="/business/contracts?new=1">
          <Plus />
          {t("business.add_contract")}
        </Link>
      </Button>
    </div>
  );
}
