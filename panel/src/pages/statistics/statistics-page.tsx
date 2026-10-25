import { useSearchParams } from "react-router";
import { useTranslation } from "react-i18next";
import { ChartColumnBig, Shapes, Store } from "lucide-react";
import { PageHeader, PanelSkeleton } from "@/components/panel";
import { PeriodPicker } from "@/components/period-picker";
import { Notice } from "@/components/notice";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useConnections, useRecurringPayments, useTransactionsBetween } from "@/hooks/use-finances";
import { usePeriodParam } from "@/hooks/use-period-param";
import { isoDate } from "@/lib/dates";
import { periodMonths, periodRange } from "@/lib/period";
import { OverviewTab } from "@/pages/statistics/overview-tab";
import { CategoriesTab } from "@/pages/statistics/categories-tab";
import { CounterpartiesTab } from "@/pages/statistics/counterparties-tab";

const TABS = ["overview", "categories", "counterparties"] as const;
type Tab = (typeof TABS)[number];

/**
 * The longer view, split into tabs so each question gets its own page:
 * how money flows, where it goes by category, and to whom.
 */
export function StatisticsPage() {
  const { t } = useTranslation();
  const [params, setParams] = useSearchParams();
  const [period, setPeriod] = usePeriodParam("rolling");
  const tab: Tab = TABS.find((entry) => entry === params.get("tab")) ?? "overview";
  const range = periodRange(period);
  const fromStartToToday = useTransactionsBetween(range.from, isoDate(new Date()));
  const connections = useConnections();
  const recurring = useRecurringPayments();
  const inPeriod = (fromStartToToday.data ?? []).filter((transaction) => transaction.date <= range.to);
  const oldest = inPeriod.reduce<string | undefined>((min, transaction) => (!min || transaction.date < min ? transaction.date : min), undefined);
  const months = periodMonths(period, new Date(), oldest);
  const setTab = (next: string) =>
    setParams(
      (previous) => {
        const updated = new URLSearchParams(previous);
        if (next === "overview") {
          updated.delete("tab");
        } else {
          updated.set("tab", next);
        }
        return updated;
      },
      { replace: true },
    );
  return (
    <>
      <PageHeader title={t("nav.statistics")} />
      {fromStartToToday.isError && <Notice className="mb-6">{t("common.error")}</Notice>}
      <Tabs value={tab} onValueChange={setTab}>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <TabsList>
            <TabsTrigger value="overview"><ChartColumnBig />{t("statistics.tab_overview")}</TabsTrigger>
            <TabsTrigger value="categories"><Shapes />{t("statistics.tab_categories")}</TabsTrigger>
            <TabsTrigger value="counterparties"><Store />{t("statistics.tab_counterparties")}</TabsTrigger>
          </TabsList>
          <PeriodPicker period={period} kinds={["quarter", "year", "rolling", "custom", "all"]} onChange={setPeriod} />
        </div>
        {fromStartToToday.data ? (
          <>
            <TabsContent value="overview">
              <OverviewTab
                transactions={inPeriod}
                untilToday={fromStartToToday.data}
                months={months}
                connections={connections.data}
                payments={recurring.data}
                from={range.from}
                to={range.to}
              />
            </TabsContent>
            <TabsContent value="categories">
              <CategoriesTab transactions={inPeriod} months={months} />
            </TabsContent>
            <TabsContent value="counterparties">
              <CounterpartiesTab transactions={inPeriod} payments={recurring.data} />
            </TabsContent>
          </>
        ) : (
          <PanelSkeleton className="mt-6 h-96" />
        )}
      </Tabs>
    </>
  );
}
