import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { Switch } from "@/components/ui/switch";
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from "@/components/ui/dialog";
import { CategoryIcon } from "@/components/category-icon";
import { Notice } from "@/components/notice";
import { useConnections } from "@/hooks/use-finances";
import { CATEGORIES } from "@/lib/categories";
import { api, type Category, type Transaction } from "@/lib/api";
import { formatDate, formatMoney } from "@/lib/format";
import { cn } from "@/lib/utils";

/**
 * A transaction up close: who, how much, when, from which account and the
 * reference, then the category as tiles and whether the choice should hold
 * for every transaction from the same counterparty.
 */
export function CategoryDialog({
  transaction,
  onClose,
}: {
  transaction: Transaction | null;
  onClose: () => void;
}) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const connections = useConnections();
  const [category, setCategory] = useState<Category>("OTHER");
  const [remember, setRemember] = useState(true);
  useEffect(() => {
    if (transaction) {
      setCategory(transaction.category);
      setRemember(Boolean(transaction.counterparty));
    }
  }, [transaction]);
  const save = useMutation({
    mutationFn: () => api.categorize(transaction!.id, category, remember),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["transactions"] }),
        queryClient.invalidateQueries({ queryKey: ["recurring"] }),
      ]);
      onClose();
    },
  });
  const account = connections.data
    ?.flatMap((connection) => connection.accounts.map((entry) => ({ ...entry, bank: connection.bank_name })))
    .find((entry) => entry.id === transaction?.account);
  return (
    <Dialog open={transaction !== null} onOpenChange={(open) => !open && onClose()}>
      {transaction && (
        <DialogContent closeLabel={t("common.close")} className="gap-6 sm:max-w-2xl">
          <div className="flex items-center gap-4 pr-8">
            <CategoryIcon category={category} className="size-12 [&_svg]:size-5" />
            <div className="min-w-0 flex-1">
              <DialogTitle className="truncate pr-0">
                {transaction.counterparty || t("transactions.no_counterparty")}
              </DialogTitle>
              <DialogDescription>{formatDate(transaction.date)}</DialogDescription>
            </div>
            <span
              className={cn(
                "figures shrink-0 text-2xl font-extrabold tracking-[-0.02em]",
                transaction.amount > 0 && "text-positive",
              )}
            >
              {formatMoney(transaction.amount, transaction.currency, { signed: true })}
            </span>
          </div>
          <dl className="grid gap-3 rounded-2xl bg-raised p-4 text-sm sm:grid-cols-[8rem_1fr]">
            {account && (
              <>
                <dt className="text-muted-foreground">{t("transactions.account")}</dt>
                <dd className="font-bold">
                  {account.bank}, {account.name}
                </dd>
              </>
            )}
            <dt className="text-muted-foreground">{t("transactions.reference")}</dt>
            <dd className="break-words">{transaction.description || "–"}</dd>
          </dl>
          <fieldset className="grid gap-3">
            <legend className="mb-3 text-sm font-bold text-label">
              {t("transactions.change_category")}
            </legend>
            <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
              {CATEGORIES.map((option) => (
                <button
                  key={option}
                  type="button"
                  aria-pressed={category === option}
                  onClick={() => setCategory(option)}
                  className={cn(
                    "flex items-center gap-2.5 rounded-xl px-2.5 py-2 text-left text-sm ring-1 ring-divider transition-colors hover:bg-raised",
                    category === option && "bg-brand/12 font-bold text-nav-active-text ring-brand/40",
                  )}
                >
                  <CategoryIcon category={option} className="size-7 bg-transparent ring-0 [&_svg]:size-4" />
                  <span className="truncate">{t(`category.${option}`)}</span>
                </button>
              ))}
            </div>
          </fieldset>
          {transaction.counterparty && (
            <label className="flex items-center justify-between gap-4 rounded-2xl bg-raised px-4 py-3 text-sm">
              <span>{t("transactions.remember", { counterparty: transaction.counterparty })}</span>
              <Switch checked={remember} onCheckedChange={setRemember} />
            </label>
          )}
          {save.isError && <Notice>{t("common.error")}</Notice>}
          <div className="flex flex-wrap justify-end gap-2">
            <DialogClose asChild>
              <Button variant="outline">{t("common.cancel")}</Button>
            </DialogClose>
            <Button disabled={save.isPending} onClick={() => save.mutate()}>
              {t("transactions.save")}
            </Button>
          </div>
        </DialogContent>
      )}
    </Dialog>
  );
}