import { createElement } from "react";
import { useTranslation } from "react-i18next";
import { Landmark, Pencil, PiggyBank } from "lucide-react";
import { Button } from "@/components/ui/button";
import type { Account, SavingsGoal } from "@/lib/api";
import { categoryIcon } from "@/lib/category-icons";
import { isoDate } from "@/lib/dates";
import { formatDate, formatMoney } from "@/lib/format";
import { goalProgress } from "@/lib/goals";

/**
 * One goal: how much is saved of the target and, with a target date, what
 * it takes per month. A goal fed by an account follows its balance, so only
 * one saved by hand offers to set money aside.
 */
export function GoalCard({
  goal,
  accounts,
  onEdit,
  onDeposit,
}: {
  goal: SavingsGoal;
  accounts: Account[];
  onEdit: () => void;
  onDeposit: () => void;
}) {
  const { t } = useTranslation();
  const progress = goalProgress(goal, accounts, isoDate(new Date()));
  const account = accounts.find((entry) => entry.id === goal.account);
  return (
    <section className="grid gap-5 rounded-3xl border bg-card p-6">
      <div className="flex items-start gap-3">
        <span className="grid size-11 shrink-0 place-items-center rounded-full bg-brand/12 text-brand">
          {createElement(categoryIcon(goal.icon), { className: "size-5", "aria-hidden": true })}
        </span>
        <span className="grid min-w-0 flex-1 gap-0.5">
          <h2 className="truncate text-lg font-extrabold">{goal.name}</h2>
          <span className="text-sm text-muted-foreground">
            {goal.target_date ? t("goals.until", { date: formatDate(goal.target_date) }) : t("goals.no_date")}
          </span>
        </span>
        <Button variant="ghost" size="icon" onClick={onEdit} aria-label={t("goals.edit")}>
          <Pencil />
        </Button>
      </div>
      <div className="grid gap-2">
        <div className="flex items-baseline justify-between gap-3">
          <span className="figures text-2xl font-extrabold">{formatMoney(progress.saved)}</span>
          <span className="figures text-sm text-muted-foreground">{t("goals.of", { target: formatMoney(goal.target) })}</span>
        </div>
        <div className="h-2 overflow-hidden rounded-full bg-raised">
          <div className="h-full rounded-full bg-primary" style={{ width: `${progress.share * 100}%` }} />
        </div>
        <span className="text-sm text-muted-foreground">
          {progress.left === 0
            ? t("goals.reached")
            : progress.perMonth !== null
              ? t("goals.per_month", { amount: formatMoney(progress.perMonth) })
              : t("goals.left", { amount: formatMoney(progress.left) })}
        </span>
      </div>
      {account ? (
        <span className="flex items-center gap-2 text-sm text-muted-foreground">
          <Landmark className="size-4" aria-hidden />
          {t("goals.linked", { account: account.name })}
        </span>
      ) : (
        <Button variant="outline" className="justify-self-start" onClick={onDeposit}>
          <PiggyBank />
          {t("goals.deposit")}
        </Button>
      )}
    </section>
  );
}
