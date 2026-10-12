import { useTranslation } from "react-i18next";
import { Panel } from "@/components/panel";
import { CashflowChart } from "@/components/charts/cashflow-chart";
import type { Transaction } from "@/lib/api";
import { lastMonths, monthlyStatistics } from "@/lib/statistics";

const TEASER_MONTHS = 6;

/** Half a year of money in and out, with the way into the statistics. */
export function CashflowTeaser({ history }: { history?: Transaction[] }) {
  const { t } = useTranslation();
  return (
    <Panel
      title={t("overview.cashflow")}
      viewAll="/statistics"
    >
      {history ? (
        <CashflowChart
          statistics={monthlyStatistics(history, lastMonths(TEASER_MONTHS))}
          height={170}
          compact
        />
      ) : (
        <div className="h-48" aria-busy="true" />
      )}
    </Panel>
  );
}