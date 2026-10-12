import { useTranslation } from "react-i18next";
import { TrendingDown, TrendingUp } from "lucide-react";
import { BalanceChart } from "@/components/charts/balance-chart";
import type { Account, Transaction } from "@/lib/api";
import { formatMoney, maskIban } from "@/lib/format";
import { total } from "@/lib/overview";
import { balanceHistory } from "@/lib/statistics";
import { cn } from "@/lib/utils";

const SPARKLINE_DAYS = 90;
const CHANGE_DAYS = 30;

/**
 * Today's balance across all accounts as the page's biggest number, how it
 * moved in 30 days, each account, and the curve over 90 days.
 */
export function BalanceHero({
  accounts,
  history,
  className,
}: {
  accounts?: Account[];
  history?: Transaction[];
  className?: string;
}) {
  const { t } = useTranslation();
  const balance = accounts ? total(accounts.map((account) => account.balance ?? 0)) : null;
  const curve =
    balance !== null && history ? balanceHistory(history, balance).slice(-SPARKLINE_DAYS) : [];
  const reference = curve.length > CHANGE_DAYS ? curve[curve.length - 1 - CHANGE_DAYS] : null;
  const change = reference && balance !== null ? balance - reference.balance : null;
  return (
    <section
      className={cn(
        "grid gap-6 rounded-3xl border bg-card p-6 lg:grid-cols-[minmax(0,1fr)_minmax(0,1.3fr)] lg:gap-10 sm:p-7",
        className,
      )}
    >
      <div className="grid content-start gap-5">
        <div className="grid gap-2">
          <h2 className="text-sm font-bold text-label">{t("overview.balance")}</h2>
          <p className="figures text-[clamp(2.4rem,6vw,3.75rem)] leading-none font-extrabold tracking-[-0.03em]">
            {balance === null ? " " : formatMoney(balance)}
          </p>
          {change !== null && (
            <span
              className={cn(
                "inline-flex w-fit items-center gap-1.5 rounded-full px-2.5 py-1 text-[13px] font-bold",
                change >= 0 ? "bg-brand/12 text-brand-text" : "bg-warning/12 text-warning",
              )}
            >
              {change >= 0 ? (
                <TrendingUp className="size-4" aria-hidden="true" />
              ) : (
                <TrendingDown className="size-4" aria-hidden="true" />
              )}
              {t("overview.change", {
                amount: formatMoney(change, "EUR", { signed: true }),
                days: CHANGE_DAYS,
              })}
            </span>
          )}
        </div>
        <ul className="divide-y divide-divider border-t border-divider">
          {accounts?.map((account) => (
            <li key={account.id} className="flex items-center justify-between gap-4 py-2.5">
              <span className="grid min-w-0">
                <span className="truncate text-sm font-bold">{account.name}</span>
                <span className="figures text-xs text-muted-foreground">{maskIban(account.iban)}</span>
              </span>
              <span className="figures text-sm font-bold">
                {account.balance === null ? "—" : formatMoney(account.balance, account.currency)}
              </span>
            </li>
          ))}
        </ul>
      </div>
      {curve.length > 1 && (
        <div className="self-end">
          <BalanceChart data={curve} height={220} label={t("overview.balance")} />
        </div>
      )}
    </section>
  );
}