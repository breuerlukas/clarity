import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { PiggyBank, RefreshCw, Trash2, Wallet } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from "@/components/ui/dialog";
import { Notice } from "@/components/notice";
import { Panel } from "@/components/panel";
import { Segmented } from "@/components/segmented";
import { api, type Account, type Connection } from "@/lib/api";
import { formatDate, formatMoney, formatRelative, maskIban } from "@/lib/format";
import { cn } from "@/lib/utils";

const DAY = 24 * 60 * 60 * 1000;
const WARNING_DAYS = 14;

/**
 * One bank with its accounts. The header says how long the bank's consent
 * lasts, each account shows whether it is everyday money or savings, and
 * the footer holds what can be done with the connection. Business accounts
 * never count towards the owner's spending money, so they have no choice
 * for it.
 */
export function ConnectionCard({
  connection,
  onReconnect,
  business = false,
}: {
  connection: Connection;
  onReconnect: () => void;
  business?: boolean;
}) {
  const { t } = useTranslation();
  const [removing, setRemoving] = useState(false);
  const [now] = useState(Date.now);
  const daysLeft = Math.ceil((connection.valid_until - now) / DAY);
  // 0 for an account that was never synced, and for a connection the bank
  // shared no account with (Math.min of nothing is Infinity).
  const oldest = connection.accounts.length ? Math.min(...connection.accounts.map((account) => account.last_synced_at)) : 0;
  return (
    <Panel title={connection.bank_name} aside={<AccessBadge daysLeft={daysLeft} validUntil={connection.valid_until} />}>
      <ul className="-my-3 divide-y divide-divider">
        {connection.accounts.map((account) => (
          <AccountRow key={account.id} account={account} business={business} />
        ))}
      </ul>
      <div className="flex flex-wrap items-center gap-x-3 gap-y-2 border-t border-divider pt-4">
        <span className="mr-auto text-sm text-muted-foreground">
          {oldest ? t("accounts.last_synced", { time: formatRelative(oldest) }) : t("accounts.never_synced")}
        </span>
        {daysLeft <= WARNING_DAYS && (
          <Button size="sm" onClick={onReconnect}>
            <RefreshCw />
            {t("accounts.renew")}
          </Button>
        )}
        <Button
          size="sm"
          variant="ghost"
          className="text-muted-foreground hover:text-destructive"
          onClick={() => setRemoving(true)}
        >
          <Trash2 />
          {t("accounts.remove")}
        </Button>
      </div>
      <RemoveDialog
        connection={connection}
        open={removing}
        onOpenChange={setRemoving}
      />
    </Panel>
  );
}

/** Quiet while the consent lasts, a warning in its last two weeks. */
function AccessBadge({ daysLeft, validUntil }: { daysLeft: number; validUntil: number }) {
  const { t } = useTranslation();
  if (daysLeft > WARNING_DAYS) {
    return <span>{t("accounts.valid_until", { date: formatDate(validUntil) })}</span>;
  }
  return (
    <span
      className={cn(
        "rounded-full px-2.5 py-0.5 text-xs font-bold",
        daysLeft <= 0 ? "bg-negative/12 text-negative" : "bg-warning/12 text-warning",
      )}
    >
      {daysLeft <= 0 ? t("accounts.expired") : t("accounts.expiring", { count: daysLeft })}
    </span>
  );
}

function AccountRow({ account, business }: { account: Account; business: boolean }) {
  const { t } = useTranslation();
  return (
    <li className="grid gap-3 py-4">
      <div className="grid grid-cols-[minmax(0,1fr)_auto] items-center gap-x-4 gap-y-2">
        <span className="grid min-w-0">
          <span className="truncate font-bold">{account.name}</span>
          {account.iban && (
            <span className="figures text-sm text-muted-foreground">{maskIban(account.iban)}</span>
          )}
        </span>
        <span className="figures text-lg font-extrabold">
          {account.balance === null
            ? "—"
            : formatMoney(account.balance, account.currency)}
        </span>
      </div>
      {!business && <SpendingChoice account={account} />}
      {account.sync_failure && (
        <Notice className="text-sm">
          {account.sync_failure === "consent"
            ? t("accounts.failure_consent")
            : t("accounts.failure_other", { reason: account.sync_failure })}
        </Notice>
      )}
    </li>
  );
}

function RemoveDialog({
  connection,
  open,
  onOpenChange,
}: {
  connection: Connection;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const remove = useMutation({
    mutationFn: () => api.removeConnection(connection.id),
    onSuccess: async () => {
      await queryClient.invalidateQueries();
      onOpenChange(false);
    },
  });
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent closeLabel={t("common.cancel")}>
        <div className="grid gap-2">
          <DialogTitle>
            {t("accounts.remove_title", { bank: connection.bank_name })}
          </DialogTitle>
          <DialogDescription>{t("accounts.remove_text")}</DialogDescription>
        </div>
        {remove.isError && <Notice>{t("common.error")}</Notice>}
        <div className="flex flex-wrap justify-end gap-2">
          <DialogClose asChild>
            <Button variant="outline">{t("common.cancel")}</Button>
          </DialogClose>
          <Button
            variant="destructive"
            disabled={remove.isPending}
            onClick={() => remove.mutate()}
          >
            {t("accounts.remove_confirm")}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
/**
 * Everyday accounts count towards the room until payday, savings don't; the
 * same words the overview shows next to each account. Shows the new choice
 * right away while it is saved.
 */
function SpendingChoice({ account }: { account: Account }) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const toggle = useMutation({
    mutationFn: (spending: boolean) => api.markSpending(account.id, spending),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["connections"] }),
  });
  return (
    <Segmented<"everyday" | "savings">
      size="sm"
      label={t("accounts.spending")}
      value={(toggle.isPending ? toggle.variables : account.spending) ? "everyday" : "savings"}
      options={[
        { value: "everyday", label: t("overview.account_everyday"), icon: Wallet },
        { value: "savings", label: t("overview.account_savings"), icon: PiggyBank },
      ]}
      onChange={(value) => toggle.mutate(value === "everyday")}
    />
  );
}
