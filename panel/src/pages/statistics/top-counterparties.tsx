import { useTranslation } from "react-i18next";
import { Panel } from "@/components/panel";
import type { Transaction } from "@/lib/api";
import { formatMoney } from "@/lib/format";
import { topCounterparties } from "@/lib/statistics";

const LIMIT = 8;

/** Where most of the money went, as ranked bars with the amount beside. */
export function TopCounterparties({ transactions }: { transactions: Transaction[] }) {
  const { t } = useTranslation();
  const top = topCounterparties(transactions, LIMIT);
  const largest = top[0]?.amount ?? 1;
  return (
    <Panel title={t("statistics.top_counterparties")}>
      {top.length === 0 && <p className="text-muted-foreground">{t("overview.categories_empty")}</p>}
      <ol className="grid gap-3">
        {top.map((entry) => (
          <li key={entry.name} className="grid gap-1.5">
            <span className="flex items-baseline justify-between gap-4 text-[15px]">
              <span className="truncate font-medium">{entry.name}</span>
              <span className="figures shrink-0 font-medium">{formatMoney(entry.amount)}</span>
            </span>
            <span className="flex items-center gap-3">
              <span className="block h-1.5 flex-1 rounded-full bg-raised">
                <span
                  className="block h-full rounded-full bg-series-out"
                  style={{ width: `${Math.max((entry.amount / largest) * 100, 1.5)}%` }}
                />
              </span>
              <span className="w-24 shrink-0 text-right text-xs text-muted-foreground">
                {t("statistics.payments", { count: entry.count })}
              </span>
            </span>
          </li>
        ))}
      </ol>
    </Panel>
  );
}