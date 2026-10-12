import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Check } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from "@/components/ui/dialog";
import { Amount } from "@/components/amount";
import { Notice } from "@/components/notice";
import { CATEGORIES } from "@/lib/categories";
import { api, type Category, type Transaction } from "@/lib/api";
import { formatDate } from "@/lib/format";
import { cn } from "@/lib/utils";

export function CategoryDialog({
  transaction,
  onClose,
}: {
  transaction: Transaction | null;
  onClose: () => void;
}) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
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
  return (
    <Dialog open={transaction !== null} onOpenChange={(open) => !open && onClose()}>
      {transaction && (
        <DialogContent closeLabel={t("common.cancel")}>
          <div className="grid gap-1">
            <DialogTitle>
              {transaction.counterparty || t("transactions.no_counterparty")}
            </DialogTitle>
            <DialogDescription className="flex flex-wrap items-baseline gap-x-3">
              <Amount
                value={transaction.amount}
                currency={transaction.currency}
                className="font-semibold text-foreground"
              />
              <span>{formatDate(transaction.date)}</span>
            </DialogDescription>
            {transaction.description && (
              <p className="mt-2 text-sm break-words text-muted-foreground">
                {transaction.description}
              </p>
            )}
          </div>
          <fieldset className="grid gap-3">
            <legend className="mb-3 text-[15px] font-medium">
              {t("transactions.change_category")}
            </legend>
            <div className="grid grid-cols-2 gap-1.5 sm:grid-cols-3">
              {CATEGORIES.map((option) => (
                <button
                  key={option}
                  type="button"
                  aria-pressed={category === option}
                  onClick={() => setCategory(option)}
                  className={cn(
                    "flex items-center justify-between gap-2 rounded-md border border-line px-3 py-2 text-left text-sm hover:border-primary/40",
                    category === option && "border-primary bg-brand/8 font-medium",
                  )}
                >
                  {t(`category.${option}`)}
                  {category === option && <Check className="size-4 shrink-0" />}
                </button>
              ))}
            </div>
          </fieldset>
          {transaction.counterparty && (
            <label className="flex items-start gap-3 text-[15px]">
              <input
                type="checkbox"
                checked={remember}
                onChange={(event) => setRemember(event.target.checked)}
                className="mt-1 size-4 accent-[var(--positive)]"
              />
              {t("transactions.remember", { counterparty: transaction.counterparty })}
            </label>
          )}
          {save.isError && <Notice>{t("common.error")}</Notice>}
          <Button size="lg" disabled={save.isPending} onClick={() => save.mutate()}>
            {t("transactions.save")}
          </Button>
        </DialogContent>
      )}
    </Dialog>
  );
}