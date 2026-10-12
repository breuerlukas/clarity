import { useTranslation } from "react-i18next";
import { Panel } from "@/components/panel";
import { CategoryIcon } from "@/components/category-icon";
import { Amount } from "@/components/amount";
import type { RecurringPayment, Transaction } from "@/lib/api";
import { formatDate } from "@/lib/format";
import { largestExpenses } from "@/lib/statistics";

const LIMIT = 8;

/**
 * The biggest one-off payments. Fixed costs are left out: rent would
 * otherwise fill the whole list every month.
 */
export function LargestExpenses({
  transactions,
  payments,
}: {
  transactions: Transaction[];
  payments?: RecurringPayment[];
}) {
  const { t } = useTranslation();
  const fixed = new Set(payments?.map((payment) => payment.counterparty));
  const expenses = largestExpenses(
    transactions.filter((transaction) => !fixed.has(transaction.counterparty)),
    LIMIT,
  );
  return (
    <Panel title={t("statistics.largest")} aside={t("statistics.largest_note")}>
      {expenses.length === 0 && <p className="text-muted-foreground">{t("overview.categories_empty")}</p>}
      <ul className="zebra -mx-2">
        {expenses.map((transaction) => (
          <li
            key={transaction.id}
            className="zebra-row grid grid-cols-[auto_1fr_auto] items-center gap-3 rounded-md px-2 py-2"
          >
            <CategoryIcon category={transaction.category} />
            <span className="grid min-w-0 gap-0.5">
              <span className="truncate font-medium">
                {transaction.counterparty || t("transactions.no_counterparty")}
              </span>
              <span className="truncate text-sm text-muted-foreground">
                {formatDate(transaction.date)}, {t(`category.${transaction.category}`)}
              </span>
            </span>
            <Amount value={transaction.amount} currency={transaction.currency} className="font-medium" />
          </li>
        ))}
      </ul>
    </Panel>
  );
}