import { useTranslation } from "react-i18next";
import { Panel } from "@/components/panel";
import type { Connection } from "@/lib/api";
import { formatMoney, maskIban } from "@/lib/format";
import { total } from "@/lib/overview";

/** Every account grouped by bank, with the total on top. */
export function AccountsCard({ connections }: { connections?: Connection[] }) {
  const { t } = useTranslation();
  const accounts = (connections ?? []).flatMap((connection) => connection.accounts);
  return (
    <Panel title={t("nav.accounts")} viewAll="/accounts">
      <div className="grid gap-1">
        <span className="text-sm text-muted-foreground">{t("overview.total")}</span>
        <span className="figures text-3xl font-extrabold tracking-[-0.02em]">
          {formatMoney(total(accounts.map((account) => account.balance ?? 0)))}
        </span>
      </div>
      <div className="grid gap-4">
        {connections?.map((connection) => (
          <div key={connection.id} className="grid gap-1.5">
            <span className="text-xs font-bold text-label">{connection.bank_name}</span>
            <ul className="divide-y divide-divider rounded-2xl bg-raised px-4">
              {connection.accounts.map((account) => (
                <li key={account.id} className="flex items-center justify-between gap-4 py-3">
                  <span className="grid min-w-0">
                    <span className="flex items-center gap-2">
                      <span className="truncate text-sm font-bold">{account.name}</span>
                      <span className="rounded-full px-1.5 text-[11px] font-bold text-muted-foreground ring-1 ring-divider ring-inset">
                        {t(account.spending ? "overview.account_everyday" : "overview.account_savings")}
                      </span>
                    </span>
                    {account.iban && (
                      <span className="figures text-xs text-muted-foreground">{maskIban(account.iban)}</span>
                    )}
                  </span>
                  <span className="figures text-sm font-bold">
                    {account.balance === null ? "–" : formatMoney(account.balance, account.currency)}
                  </span>
                </li>
              ))}
            </ul>
          </div>
        ))}
      </div>
    </Panel>
  );
}
