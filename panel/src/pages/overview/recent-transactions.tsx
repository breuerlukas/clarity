import { useTranslation } from "react-i18next";
import { Panel } from "@/components/panel";
import { Ledger, LedgerRow } from "@/components/ledger";
import type { Transaction } from "@/lib/api";
import { currentMonth, type MonthKey } from "@/lib/dates";

const VISIBLE_ROWS = 6;

export function RecentTransactions({
  month,
  transactions,
}: {
  month: MonthKey;
  transactions?: Transaction[];
}) {
  const { t } = useTranslation();
  const link =
    month === currentMonth() ? "/transactions" : `/transactions?month=${month}`;
  return (
    <Panel
      title={t("overview.recent")}
      viewAll={link}
    >
      {transactions?.length === 0 && (
        <p className="text-muted-foreground">{t("transactions.empty")}</p>
      )}
      <Ledger>
        {transactions?.slice(0, VISIBLE_ROWS).map((transaction) => (
          <LedgerRow key={transaction.id} transaction={transaction} />
        ))}
      </Ledger>
    </Panel>
  );
}