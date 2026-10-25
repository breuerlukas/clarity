import { useState, type FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogTitle } from "@/components/ui/dialog";
import { Field } from "@/components/field";
import { Notice } from "@/components/notice";
import { useSpace } from "@/hooks/use-space";
import { api, ApiError } from "@/lib/api";
import type { MonthlyBudget } from "@/lib/budget";

/**
 * Sets what the owner wants to spend per pay cycle beyond fixed costs, or
 * goes back to the suggestion of salary minus fixed costs.
 */
export function MonthlyBudgetDialog({ open, budget, onClose }: { open: boolean; budget: MonthlyBudget | null; onClose: () => void }) {
  const { t } = useTranslation();
  return (
    <Dialog open={open} onOpenChange={(isOpen) => !isOpen && onClose()}>
      {open && (
        <DialogContent closeLabel={t("common.cancel")}>
          <BudgetForm budget={budget} onClose={onClose} />
        </DialogContent>
      )}
    </Dialog>
  );
}

function BudgetForm({ budget, onClose }: { budget: MonthlyBudget | null; onClose: () => void }) {
  const { t } = useTranslation();
  const space = useSpace();
  const queryClient = useQueryClient();
  const [amount, setAmount] = useState(space.monthly_budget?.toString() ?? "");
  const save = useMutation({
    mutationFn: (value: number | null) => api.changeMonthlyBudget(space.id, value),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["spaces"] });
      onClose();
    },
  });
  const submit = (event: FormEvent) => {
    event.preventDefault();
    save.mutate(Number(amount));
  };
  return (
    <form onSubmit={submit} className="grid gap-4">
      <div className="grid gap-2">
        <DialogTitle>{t("room.budget_title")}</DialogTitle>
        <DialogDescription>{t("room.budget_hint")}</DialogDescription>
      </div>
      <Field
        label={t("room.budget_amount")}
        type="number"
        inputMode="decimal"
        min="0.01"
        step="0.01"
        value={amount}
        onChange={(event) => setAmount(event.target.value)}
        placeholder={budget?.suggested ? String(budget.limit) : undefined}
        required
        autoFocus
      />
      {save.isError && (
        <Notice>{t(`room.errors.${save.error instanceof ApiError ? save.error.code : ""}`, { defaultValue: t("common.error") })}</Notice>
      )}
      <div className="flex flex-wrap items-center justify-end gap-2">
        {space.monthly_budget !== null && (
          <Button type="button" variant="link" className="mr-auto px-0 text-muted-foreground" disabled={save.isPending} onClick={() => save.mutate(null)}>
            {t("room.use_suggestion")}
          </Button>
        )}
        <Button type="button" variant="outline" onClick={onClose}>
          {t("common.cancel")}
        </Button>
        <Button type="submit" loading={save.isPending} disabled={!(Number(amount) > 0)}>
          {t("common.save")}
        </Button>
      </div>
    </form>
  );
}
