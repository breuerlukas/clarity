import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useMutation, useQueryClient } from "@tanstack/react-query";
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
import { api, type Account, type Connection } from "@/lib/api";
import { formatDate, formatMoney, formatRelative, maskIban } from "@/lib/format";

const DAY = 24 * 60 * 60 * 1000;
const WARNING_DAYS = 14;

export function ConnectionCard({
  connection,
  onReconnect,
}: {
  connection: Connection;
  onReconnect: () => void;
}) {
  const { t } = useTranslation();
  const [removing, setRemoving] = useState(false);
  const daysLeft = Math.ceil((connection.valid_until - Date.now()) / DAY);
  return (
    <Panel
      title={connection.bank_name}
      aside={t("accounts.valid_until", { date: formatDate(connection.valid_until) })}
    >
      {daysLeft <= WARNING_DAYS && (
        <div className="flex flex-wrap items-center gap-3">
          <Notice>
            {daysLeft <= 0
              ? t("accounts.expired")
              : t("accounts.expiring", { count: daysLeft })}
          </Notice>
          <Button size="sm" variant="outline" onClick={onReconnect}>
            {t("accounts.connect")}
          </Button>
        </div>
      )}
      <ul className="zebra -mx-2">
        {connection.accounts.map((account) => (
          <AccountRow key={account.id} account={account} />
        ))}
      </ul>
      <Button
        variant="link"
        size="sm"
        className="self-start px-0 text-muted-foreground"
        onClick={() => setRemoving(true)}
      >
        {t("accounts.remove")}
      </Button>
      <RemoveDialog
        connection={connection}
        open={removing}
        onOpenChange={setRemoving}
      />
    </Panel>
  );
}

function AccountRow({ account }: { account: Account }) {
  const { t } = useTranslation();
  const status = account.sync_failure
    ? null
    : account.last_synced_at
      ? t("accounts.last_synced", { time: formatRelative(account.last_synced_at) })
      : t("accounts.never_synced");
  return (
    <li className="zebra-row grid gap-1 rounded-md px-2 py-3">
      <div className="grid grid-cols-[1fr_auto] items-baseline gap-4">
        <span className="min-w-0 truncate font-medium">{account.name}</span>
        <span className="figures font-semibold">
          {account.balance === null
            ? "—"
            : formatMoney(account.balance, account.currency)}
        </span>
      </div>
      <div className="flex flex-wrap justify-between gap-x-4 text-sm text-muted-foreground">
        <span className="figures">{maskIban(account.iban)}</span>
        {status && <span>{status}</span>}
      </div>
      {account.sync_failure && (
        <Notice className="mt-1 text-sm">
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