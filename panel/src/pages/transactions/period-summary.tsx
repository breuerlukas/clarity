import { useTranslation } from "react-i18next";
import { Panel } from "@/components/panel";
import { AmountBadge } from "@/components/amount-badge";
import { CategoryIcon } from "@/components/category-icon";
import type { Category, Transaction } from "@/lib/api";
import { formatMoney } from "@/lib/format";
import { summarizeMonth } from "@/lib/overview";
import { cn } from "@/lib/utils";

/**
 * Beside the list on wide screens: the period's money in and out, and its
 * categories as filters (a second click clears the filter).
 */
export function PeriodSummary({
  transactions,
  category,
  onCategoryChange,
}: {
  transactions?: Transaction[];
  category: Category | null;
  onCategoryChange: (category: Category | null) => void;
}) {
  const { t } = useTranslation();
  if (!transactions) {
    return null;
  }
  const summary = summarizeMonth(transactions);
  return (
    <Panel title={t("transactions.period_summary")} className="hidden 2xl:flex">
      <div className="flex flex-wrap gap-2">
        <AmountBadge value={summary.income} className="px-3 py-1 text-sm" />
        <AmountBadge value={-summary.spending} className="px-3 py-1 text-sm" />
      </div>
      <ul className="grid gap-0.5 border-t border-divider pt-4">
        {summary.categories.map((entry) => (
          <li key={entry.category}>
            <button
              type="button"
              aria-pressed={category === entry.category}
              onClick={() => onCategoryChange(category === entry.category ? null : entry.category)}
              className={cn(
                "grid w-full grid-cols-[auto_1fr_auto] items-center gap-3 rounded-xl px-2 py-1.5 text-left text-sm hover:bg-raised",
                category === entry.category && "bg-brand/12 font-bold text-nav-active-text",
              )}
            >
              <CategoryIcon category={entry.category} className="size-8" />
              <span className="truncate">{t(`category.${entry.category}`)}</span>
              <span className="figures font-bold">{formatMoney(entry.amount)}</span>
            </button>
          </li>
        ))}
      </ul>
    </Panel>
  );
}
