import { useTranslation } from "react-i18next";
import { Panel } from "@/components/panel";
import { Skeleton } from "@/components/ui/skeleton";
import { ActivityHeatmap } from "@/components/charts/activity-heatmap";
import type { Transaction } from "@/lib/api";
import { isoDate } from "@/lib/dates";

const WEEKS = 26;

/** The last half year, one square per day. */
export function ActivityCard({ transactions }: { transactions?: Transaction[] }) {
  const { t } = useTranslation();
  const today = new Date();
  const from = new Date(today);
  from.setDate(from.getDate() - WEEKS * 7 + 1);
  return (
    <Panel title={t("statistics.activity")}>
      {transactions ? (
        <ActivityHeatmap transactions={transactions} from={isoDate(from)} to={isoDate(today)} />
      ) : (
        <Skeleton className="h-48 rounded-2xl" aria-busy="true" />
      )}
    </Panel>
  );
}
