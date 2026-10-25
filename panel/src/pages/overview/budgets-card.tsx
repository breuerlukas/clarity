import { useTranslation } from "react-i18next";
import { Panel } from "@/components/panel";
import { CategoryIcon } from "@/components/category-icon";
import { useCategoryLook } from "@/hooks/use-categories";
import { useOpenSettings } from "@/layouts/settings/settings-dialog-context";
import type { BudgetUsage } from "@/lib/budget";
import { formatMoney } from "@/lib/format";
import { cn } from "@/lib/utils";

/**
 * Spent against the limit of every budgeted category in this pay cycle;
 * bars turn amber close to the limit and red beyond it.
 */
export function BudgetsCard({ usage }: { usage: BudgetUsage[] }) {
  const { t } = useTranslation();
  const look = useCategoryLook();
  const openSettings = useOpenSettings();
  return (
    <Panel
      title={t("budgets.title")}
      aside={
        <button
          type="button"
          onClick={() => openSettings("categories")}
          className="text-sm text-muted-foreground underline-offset-4 hover:text-foreground hover:underline"
        >
          {t("budgets.manage")}
        </button>
      }
    >
      <ul className="grid gap-4">
        {usage.map((budget) => (
          <li key={budget.category} className="grid grid-cols-[auto_1fr] items-center gap-x-3 gap-y-1.5">
            <CategoryIcon category={budget.category} className="row-span-2" />
            <span className="flex items-baseline justify-between gap-3 text-sm">
              <span className="truncate font-medium">{look(budget.category).label}</span>
              <span className="figures shrink-0 text-muted-foreground">
                {t("budgets.of_limit", { spent: formatMoney(budget.spent), limit: formatMoney(budget.limit) })}
              </span>
            </span>
            <span className="grid gap-1">
              <span className="h-2 overflow-hidden rounded-full bg-raised">
                <span
                  className={cn(
                    "block h-full rounded-full bg-primary",
                    budget.state === "close" && "bg-warning",
                    budget.state === "over" && "bg-negative",
                  )}
                  style={{ width: `${Math.min(1, budget.share) * 100}%` }}
                />
              </span>
              <span className={cn("text-xs text-muted-foreground", budget.state === "over" && "font-bold text-negative")}>
                {budget.state === "over"
                  ? t("budgets.over", { amount: formatMoney(budget.spent - budget.limit) })
                  : t("budgets.left", { amount: formatMoney(budget.limit - budget.spent) })}
              </span>
            </span>
          </li>
        ))}
      </ul>
    </Panel>
  );
}
