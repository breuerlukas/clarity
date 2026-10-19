import { useTranslation } from "react-i18next";
import { Panel } from "@/components/panel";
import { BalanceChart } from "@/components/charts/balance-chart";
import { AmountBadge } from "@/components/amount-badge";
import type { Account, Transaction } from "@/lib/api";
import { total } from "@/lib/overview";
import { balanceHistory } from "@/lib/statistics";

const DAYS = 90;
const CHANGE_DAYS = 30;

/**
 * All bank accounts together over the last 90 days, and the change in 30.
 * Cash entries stay out: they never touched these balances.
 */
export function BalanceCard({
  accounts,
  history,
}: {
  accounts: Account[];
  history: Transaction[];
}) {
  const { t } = useTranslation();
  const balance = total(accounts.map((account) => account.balance ?? 0));
  const banked = new Set(accounts.map((account) => account.id));
  const curve = balanceHistory(
    history.filter((transaction) => banked.has(transaction.account)),
    balance,
  ).slice(-DAYS);
  const reference = curve.length > CHANGE_DAYS ? curve[curve.length - 1 - CHANGE_DAYS] : null;
  return (
    <Panel
      title={t("overview.balance_title")}
      aside={
        reference ? (
          <span className="flex items-center gap-2">
            <AmountBadge value={total([balance, -reference.balance])} />
            {t("overview.in_days", { days: CHANGE_DAYS })}
          </span>
        ) : undefined
      }
    >
      {curve.length > 1 ? (
        <BalanceChart data={curve} height={240} label={t("overview.balance_title")} />
      ) : (
        <p className="text-muted-foreground">{t("statistics.not_enough")}</p>
      )}
    </Panel>
  );
}
