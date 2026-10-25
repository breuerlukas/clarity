import { useState, type FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogTitle } from "@/components/ui/dialog";
import { Field } from "@/components/field";
import { Notice } from "@/components/notice";
import { api, ApiError, type SavingsGoal } from "@/lib/api";
import { formatMoney } from "@/lib/format";

/** Sets money aside for a goal, or takes some of it back. */
export function DepositDialog({ goal, onClose }: { goal: SavingsGoal | null; onClose: () => void }) {
  const { t } = useTranslation();
  return (
    <Dialog open={goal !== null} onOpenChange={(open) => !open && onClose()}>
      {goal && (
        <DialogContent closeLabel={t("common.cancel")}>
          <DepositForm key={goal.id} goal={goal} onClose={onClose} />
        </DialogContent>
      )}
    </Dialog>
  );
}

function DepositForm({ goal, onClose }: { goal: SavingsGoal; onClose: () => void }) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const [amount, setAmount] = useState("");
  const deposit = useMutation({
    mutationFn: (sign: 1 | -1) => api.depositToGoal(goal.id, sign * Number(amount)),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["goals"] });
      onClose();
    },
  });
  const submit = (event: FormEvent) => {
    event.preventDefault();
    deposit.mutate(1);
  };
  const valid = Number(amount) > 0;
  return (
    <>
      <div className="grid gap-2">
        <DialogTitle>{goal.name}</DialogTitle>
        <DialogDescription>{t("goals.deposit_hint", { saved: formatMoney(goal.saved) })}</DialogDescription>
      </div>
      <form onSubmit={submit} className="grid gap-4">
        <Field
          label={t("goals.amount")}
          type="number"
          inputMode="decimal"
          min="0.01"
          step="0.01"
          value={amount}
          onChange={(event) => setAmount(event.target.value)}
          required
          autoFocus
        />
        {deposit.isError && (
          <Notice>{t(`goals.errors.${deposit.error instanceof ApiError ? deposit.error.code : ""}`, { defaultValue: t("common.error") })}</Notice>
        )}
        <div className="flex flex-wrap justify-end gap-2">
          <Button type="button" variant="outline" disabled={!valid || deposit.isPending || goal.saved <= 0} onClick={() => deposit.mutate(-1)}>
            {t("goals.withdraw")}
          </Button>
          <Button type="submit" loading={deposit.isPending} disabled={!valid}>
            {t("goals.deposit")}
          </Button>
        </div>
      </form>
    </>
  );
}
