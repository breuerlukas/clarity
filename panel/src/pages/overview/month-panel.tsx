import { Link } from "react-router";
import { Trans, useTranslation } from "react-i18next";
import { Panel } from "@/components/panel";
import { MonthSwitcher } from "@/components/month-switcher";
import { CategoryIcon } from "@/components/category-icon";
import type { Transaction } from "@/lib/api";
import { currentMonth, type MonthKey } from "@/lib/dates";
import { formatMoney, formatMonth } from "@/lib/format";
import { summarizeMonth, type MonthSummary } from "@/lib/overview";

/**
 * One month in a sentence, one bar for how much of the money in went out,
 * then the categories it went to. Each category opens its transactions.
 */
export function MonthPanel({
  month,
  onMonthChange,
  transactions,
}: {
  month: MonthKey;
  onMonthChange: (month: MonthKey) => void;
  transactions?: Transaction[];
}) {
  const summary = transactions ? summarizeMonth(transactions) : null;
  return (
    <Panel>
      <div className="-mt-2 -ml-3">
        <MonthSwitcher month={month} onChange={onMonthChange} />
      </div>
      {summary ? (
        <>
          <Flow month={month} summary={summary} />
          <Categories month={month} summary={summary} />
        </>
      ) : (
        <div className="h-64" aria-busy="true" />
      )}
    </Panel>
  );
}

function Flow({ month, summary }: { month: MonthKey; summary: MonthSummary }) {
  const { t } = useTranslation();
  const difference = summary.income - summary.spending;
  const share =
    summary.income > 0
      ? Math.round((summary.spending / summary.income) * 100)
      : null;
  const strong = <strong className="figures font-semibold" />;
  return (
    <div className="grid gap-3">
      <p className="max-w-[36rem] text-[17px] leading-relaxed">
        <Trans
          i18nKey="overview.month_summary"
          values={{
            month: formatMonth(month),
            income: formatMoney(summary.income),
            spending: formatMoney(summary.spending),
          }}
          components={{ strong }}
        />{" "}
        {difference >= 0 && (
          <Trans
            i18nKey="overview.left_over"
            values={{ difference: formatMoney(difference) }}
            components={{ strong }}
          />
        )}
      </p>
      {share !== null && (
        <div className="grid gap-1.5">
          <div
            className="h-2 overflow-hidden rounded-full bg-raised ring-1 ring-line"
            role="img"
            aria-label={t("overview.spent_share", { percent: share })}
          >
            <div
              className={`h-full rounded-full ${share > 100 ? "bg-warning" : "bg-primary"}`}
              style={{ width: `${Math.min(share, 100)}%` }}
            />
          </div>
          <p className={`text-sm ${share > 100 ? "text-warning" : "text-muted-foreground"}`}>
            {share > 100
              ? t("overview.overspent")
              : t("overview.spent_share", { percent: share })}
          </p>
        </div>
      )}
    </div>
  );
}

function Categories({
  month,
  summary,
}: {
  month: MonthKey;
  summary: MonthSummary;
}) {
  const { t } = useTranslation();
  const largest = summary.categories[0]?.amount ?? 0;
  const monthQuery = month === currentMonth() ? "" : `month=${month}&`;
  if (summary.categories.length === 0) {
    return <p className="text-muted-foreground">{t("overview.categories_empty")}</p>;
  }
  return (
    <div className="grid gap-2 border-t border-line pt-4">
      <h3 className="text-[15px] font-semibold">{t("overview.categories")}</h3>
      <ul className="grid gap-0.5">
        {summary.categories.map(({ category, amount }) => (
          <li key={category}>
            <Link
              to={`/transactions?${monthQuery}category=${category}`}
              title={`${Math.round((amount / summary.spending) * 100)} %`}
              className="grid grid-cols-[auto_1fr] items-center gap-3 rounded-md px-1 py-1.5 hover:bg-raised"
            >
              <CategoryIcon category={category} className="size-8" />
              <span className="grid gap-1.5">
                <span className="flex items-baseline justify-between gap-4 text-[15px]">
                  <span>{t(`category.${category}`)}</span>
                  <span className="figures font-medium">{formatMoney(amount)}</span>
                </span>
                <span className="block h-1.5 rounded-full bg-raised" aria-hidden="true">
                  <span
                    className="block h-full rounded-full bg-series-out"
                    style={{ width: `${Math.max((amount / largest) * 100, 1.5)}%` }}
                  />
                </span>
              </span>
            </Link>
          </li>
        ))}
      </ul>
    </div>
  );
}