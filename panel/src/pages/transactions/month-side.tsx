import { useTranslation } from "react-i18next";
import { Panel } from "@/components/panel";
import { CategoryIcon } from "@/components/category-icon";
import type { Category, Transaction } from "@/lib/api";
import { formatMoney } from "@/lib/format";
import { summarizeMonth } from "@/lib/overview";
import { cn } from "@/lib/utils";

/**
 * On wide screens, the month's totals beside the list, and its categories
 * as filters: one click narrows the list, a second one clears it.
 */
export function MonthSide({
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
    <Panel title={t("transactions.month_summary")} className="hidden 2xl:flex">
      <dl className="grid grid-cols-2 gap-3">
        <Figure label={t("statistics.money_in")} value={formatMoney(summary.income)} />
        <Figure label={t("statistics.money_out")} value={formatMoney(summary.spending)} />
      </dl>
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

function Figure({ label, value }: { label: string; value: string }) {
  return (
    <div className="grid gap-1 rounded-2xl bg-raised p-3.5">
      <dt className="text-xs font-bold text-label">{label}</dt>
      <dd className="figures text-lg font-extrabold">{value}</dd>
    </div>
  );
}
