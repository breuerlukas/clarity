import { Link } from "react-router";
import { useTranslation } from "react-i18next";
import { Panel } from "@/components/panel";
import { CategoryIcon } from "@/components/category-icon";
import type { Transaction } from "@/lib/api";
import { currentMonth, type MonthKey } from "@/lib/dates";
import { formatMoney, formatMonthShort } from "@/lib/format";
import { categoryMatrix } from "@/lib/statistics";

/**
 * Spending per category and month as a table whose cells darken with the
 * amount (one hue, light to dark), so outliers jump out while the exact
 * figure stays readable in every cell.
 */
export function CategoryTable({
  transactions,
  months,
}: {
  transactions: Transaction[];
  months: MonthKey[];
}) {
  const { t } = useTranslation();
  const rows = categoryMatrix(transactions, months);
  const largest = Math.max(1, ...rows.flatMap((row) => row.amounts));
  return (
    <Panel title={t("statistics.categories")}>
      {rows.length === 0 ? (
        <p className="text-muted-foreground">{t("overview.categories_empty")}</p>
      ) : (
        <div className="-mx-5 overflow-x-auto px-5 sm:-mx-6 sm:px-6">
          <table className="w-full min-w-[40rem] border-separate border-spacing-0.5 text-sm">
            <thead>
              <tr className="text-muted-foreground">
                <th scope="col" className="pb-2 text-left font-medium">
                  <span className="sr-only">{t("statistics.category")}</span>
                </th>
                {months.map((month) => (
                  <th key={month} scope="col" className="pb-2 text-right font-medium">
                    {formatMonthShort(month)}
                  </th>
                ))}
                <th scope="col" className="pb-2 pl-3 text-right font-medium">
                  {t("statistics.average")}
                </th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.category}>
                  <th scope="row" className="py-0.5 pr-3 text-left font-normal">
                    <span className="flex items-center gap-2.5">
                      <CategoryIcon category={row.category} className="size-7" />
                      <span className="whitespace-nowrap">{t(`category.${row.category}`)}</span>
                    </span>
                  </th>
                  {row.amounts.map((amount, index) => (
                    <td
                      key={months[index]}
                      className="figures rounded-sm px-2 py-1.5 text-right whitespace-nowrap"
                      style={{
                        backgroundColor: `color-mix(in oklab, var(--series-out) ${Math.round((amount / largest) * 55)}%, transparent)`,
                      }}
                    >
                      {amount === 0 ? (
                        <span className="text-muted-foreground/60">–</span>
                      ) : (
                        <Link
                          to={`/transactions?${months[index] === currentMonth() ? "" : `month=${months[index]}&`}category=${row.category}`}
                          className="hover:underline"
                        >
                          {formatMoney(amount, "EUR", { compact: amount >= 10000 })}
                        </Link>
                      )}
                    </td>
                  ))}
                  <td className="figures py-1.5 pl-3 text-right font-semibold whitespace-nowrap">
                    {formatMoney(row.average)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Panel>
  );
}