import { useTranslation } from "react-i18next";
import { Panel } from "@/components/panel";
import { Ledger, LedgerRow } from "@/components/ledger";
import { useSpacePath } from "@/hooks/use-space";
import type { Transaction } from "@/lib/api";

const VISIBLE = 6;

export function RecentTransactions({
  transactions,
  className,
}: {
  transactions?: Transaction[];
  className?: string;
}) {
  const { t } = useTranslation();
  const path = useSpacePath();
  return (
    <Panel title={t("overview.recent")} viewAll={path("/transactions")} className={className}>
      {transactions?.length === 0 && <p className="text-muted-foreground">{t("transactions.empty")}</p>}
      <Ledger>
        {transactions?.slice(0, VISIBLE).map((transaction) => (
          <LedgerRow key={transaction.id} transaction={transaction} />
        ))}
      </Ledger>
    </Panel>
  );
}
