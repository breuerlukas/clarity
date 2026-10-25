import { useId, useState, type FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogTitle } from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Field } from "@/components/field";
import { Notice } from "@/components/notice";
import { Segmented } from "@/components/segmented";
import { useSpace } from "@/hooks/use-space";
import { api, ApiError, type Account, type SavingsGoal, type SavingsGoalDraft } from "@/lib/api";
import { CATEGORY_ICONS } from "@/lib/category-icons";
import { cn } from "@/lib/utils";

type Source = "manual" | "account";

/**
 * Creates or changes a goal: name, icon, target, an optional date, and
 * whether the money is set aside by hand or is the balance of an account.
 */
export function GoalDialog({
  open,
  goal,
  accounts,
  onClose,
}: {
  open: boolean;
  goal: SavingsGoal | null;
  accounts: Account[];
  onClose: () => void;
}) {
  const { t } = useTranslation();
  return (
    <Dialog open={open} onOpenChange={(isOpen) => !isOpen && onClose()}>
      {open && (
        <DialogContent closeLabel={t("common.cancel")}>
          <GoalForm key={goal?.id ?? "new"} goal={goal} accounts={accounts} onClose={onClose} />
        </DialogContent>
      )}
    </Dialog>
  );
}

function GoalForm({ goal, accounts, onClose }: { goal: SavingsGoal | null; accounts: Account[]; onClose: () => void }) {
  const { t } = useTranslation();
  const space = useSpace();
  const queryClient = useQueryClient();
  const accountId = useId();
  const [name, setName] = useState(goal?.name ?? "");
  const [icon, setIcon] = useState(goal?.icon ?? "piggy-bank");
  const [target, setTarget] = useState(goal ? String(goal.target) : "");
  const [targetDate, setTargetDate] = useState(goal?.target_date ?? "");
  const [source, setSource] = useState<Source>(goal?.account ? "account" : "manual");
  const [account, setAccount] = useState(goal?.account ?? accounts.find((entry) => !entry.spending)?.id ?? accounts[0]?.id ?? "");
  const draft: SavingsGoalDraft = {
    name: name.trim(),
    icon,
    target: Number(target),
    target_date: targetDate || null,
    account: source === "account" ? account || null : null,
  };
  const done = async () => {
    await queryClient.invalidateQueries({ queryKey: ["goals"] });
    onClose();
  };
  const save = useMutation({
    mutationFn: () => (goal ? api.updateGoal(goal.id, draft) : api.createGoal(space.id, draft)),
    onSuccess: done,
  });
  const remove = useMutation({ mutationFn: () => api.deleteGoal(goal!.id), onSuccess: done });
  const submit = (event: FormEvent) => {
    event.preventDefault();
    save.mutate();
  };
  const error = save.error ?? remove.error;
  return (
    <form onSubmit={submit} className="grid gap-4">
      <DialogTitle>{t(goal ? "goals.edit" : "goals.create")}</DialogTitle>
      <Field label={t("goals.name")} value={name} onChange={(event) => setName(event.target.value)} maxLength={60} placeholder={t("goals.name_placeholder")} required />
      <fieldset className="grid gap-2">
        <legend className="mb-2 text-sm font-bold text-label">{t("goals.icon")}</legend>
        <div className="grid grid-cols-8 gap-1.5">
          {CATEGORY_ICONS.map((option) => (
            <button
              key={option.id}
              type="button"
              aria-label={option.id}
              aria-pressed={icon === option.id}
              onClick={() => setIcon(option.id)}
              className={cn(
                "grid aspect-square place-items-center rounded-xl ring-1 ring-divider transition-colors hover:bg-raised",
                icon === option.id && "bg-brand/12 text-nav-active-text ring-brand/40",
              )}
            >
              <option.icon className="size-4" aria-hidden />
            </button>
          ))}
        </div>
      </fieldset>
      <div className="grid gap-4 sm:grid-cols-2">
        <Field label={t("goals.target")} type="number" inputMode="decimal" min="0.01" step="0.01" value={target} onChange={(event) => setTarget(event.target.value)} required />
        <Field label={t("goals.target_date")} type="date" value={targetDate} onChange={(event) => setTargetDate(event.target.value)} hint={t("goals.target_date_hint")} />
      </div>
      <div className="grid gap-2">
        <span className="text-sm font-bold text-label">{t("goals.source")}</span>
        <Segmented<Source>
          label={t("goals.source")}
          value={source}
          options={[
            { value: "manual", label: t("goals.source_manual") },
            { value: "account", label: t("goals.source_account") },
          ]}
          onChange={setSource}
        />
      </div>
      {source === "account" && (
        <div className="grid gap-2">
          <Label htmlFor={accountId}>{t("goals.account")}</Label>
          <select
            id={accountId}
            value={account}
            onChange={(event) => setAccount(event.target.value)}
            className="h-12 rounded-2xl border-[1.5px] border-transparent bg-ground px-4 text-sm text-foreground outline-none focus-visible:border-primary"
          >
            {accounts.map((option) => (
              <option key={option.id} value={option.id}>
                {option.name}
              </option>
            ))}
          </select>
        </div>
      )}
      {error && <Notice>{t(`goals.errors.${error instanceof ApiError ? error.code : ""}`, { defaultValue: t("common.error") })}</Notice>}
      <div className="flex flex-wrap items-center justify-end gap-2">
        {goal && (
          <Button type="button" variant="link" className="mr-auto px-0 text-muted-foreground" disabled={remove.isPending} onClick={() => remove.mutate()}>
            {t("goals.delete")}
          </Button>
        )}
        <Button type="button" variant="outline" onClick={onClose}>
          {t("common.cancel")}
        </Button>
        <Button type="submit" loading={save.isPending} disabled={!draft.name || !(draft.target > 0) || (source === "account" && !account)}>
          {t("common.save")}
        </Button>
      </div>
    </form>
  );
}
