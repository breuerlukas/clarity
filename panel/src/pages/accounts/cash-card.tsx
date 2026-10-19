import { useState, type FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { useMutation } from "@tanstack/react-query";
import { Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Field } from "@/components/field";
import { Notice } from "@/components/notice";
import { Panel } from "@/components/panel";
import { useCashWallet, useRefreshCash } from "@/hooks/use-cash";
import { useSpace } from "@/hooks/use-space";
import { CashEntryDialog } from "@/pages/transactions/cash-entry-dialog";
import { api } from "@/lib/api";
import { formatDate, formatMoney } from "@/lib/format";

/**
 * The cash wallet beside the bank accounts: what should be in it, a quick
 * cash entry, and counting it to correct the balance.
 */
export function CashCard() {
  const { t } = useTranslation();
  const space = useSpace();
  const wallet = useCashWallet();
  const refresh = useRefreshCash();
  const [adding, setAdding] = useState(false);
  const [counted, setCounted] = useState<string | null>(null);
  const count = useMutation({
    mutationFn: () => api.countCash(space.id, Math.round(Number(counted!.replace(",", ".")) * 100) / 100),
    onSuccess: async () => {
      await refresh();
      setCounted(null);
    },
  });
  const submit = (event: FormEvent) => {
    event.preventDefault();
    count.mutate();
  };
  return (
    <Panel title={t("cash.title")}>
      <div className="grid gap-1">
        {wallet.data ? (
          <>
            <span className="figures text-3xl font-extrabold tracking-[-0.02em]">
              {formatMoney(wallet.data.balance, wallet.data.currency)}
            </span>
            <span className="text-sm text-muted-foreground">
              {t("cash.since", { date: formatDate(wallet.data.started_on) })}
            </span>
          </>
        ) : (
          <Skeleton className="h-9 w-44" />
        )}
      </div>
      {wallet.isError && <Notice>{t("common.error")}</Notice>}
      {counted === null ? (
        <div className="flex flex-wrap gap-2">
          <Button variant="outline" onClick={() => setAdding(true)}>
            <Plus />
            {t("cash.add_entry")}
          </Button>
          <Button variant="outline" disabled={!wallet.data} onClick={() => setCounted("")}>
            {t("cash.count")}
          </Button>
        </div>
      ) : (
        <form onSubmit={submit} className="grid gap-3">
          <Field
            label={t("cash.counted")}
            hint={t("cash.count_hint")}
            value={counted}
            onChange={(event) => setCounted(event.target.value)}
            inputMode="decimal"
            pattern="[0-9]+([.,][0-9]{1,2})?"
            required
            autoFocus
          />
          {count.isError && <Notice>{t("common.error")}</Notice>}
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={() => setCounted(null)}>
              {t("common.cancel")}
            </Button>
            <Button type="submit" loading={count.isPending}>
              {t("common.save")}
            </Button>
          </div>
        </form>
      )}
      <CashEntryDialog open={adding} entry={null} onOpenChange={setAdding} />
    </Panel>
  );
}
