import { useMemo, useState } from "react";
import { useSearchParams } from "react-router";
import { useTranslation } from "react-i18next";
import { Search } from "lucide-react";
import { Input } from "@/components/ui/input";
import { PageHeader, Panel } from "@/components/panel";
import { MonthSwitcher } from "@/components/month-switcher";
import { Notice } from "@/components/notice";
import { Ledger, LedgerDate, LedgerRow } from "@/components/ledger";
import { useMonthTransactions } from "@/hooks/use-finances";
import { useMonthParam } from "@/hooks/use-month-param";
import { CategoryDialog } from "@/pages/transactions/category-dialog";
import { CategorySelect } from "@/pages/transactions/category-select";
import { MonthSide } from "@/pages/transactions/month-side";
import type { Category, Transaction } from "@/lib/api";
import { formatDay } from "@/lib/format";

export function TransactionsPage() {
  const { t } = useTranslation();
  const [month, setMonth] = useMonthParam();
  const [params, setParams] = useSearchParams();
  const [query, setQuery] = useState("");
  const [selected, setSelected] = useState<Transaction | null>(null);
  const category = params.get("category") as Category | null;
  const transactions = useMonthTransactions(month);
  const visible = useMemo(
    () => filter(transactions.data ?? [], query, category),
    [transactions.data, query, category],
  );
  const setCategory = (next: Category | null) =>
    setParams((previous) => {
      const updated = new URLSearchParams(previous);
      if (next) {
        updated.set("category", next);
      } else {
        updated.delete("category");
      }
      return updated;
    });
  return (
    <>
      <PageHeader title={t("transactions.title")} />
      <div className="grid items-start gap-6 2xl:grid-cols-[minmax(0,1fr)_400px]">
        <Panel>
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="-ml-3">
              <MonthSwitcher month={month} onChange={setMonth} />
            </div>
            <div className="flex w-full gap-3 sm:w-auto">
              <label className="relative min-w-0 flex-1 sm:w-64">
                <span className="sr-only">{t("transactions.search")}</span>
                <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
                <Input
                  type="search"
                  value={query}
                  onChange={(event) => setQuery(event.target.value)}
                  placeholder={t("transactions.search_placeholder")}
                  className="pl-9"
                />
              </label>
              <CategorySelect value={category} onChange={setCategory} />
            </div>
          </div>
          {transactions.isError && <Notice>{t("common.error")}</Notice>}
          {transactions.data && visible.length === 0 && (
            <p className="py-6 text-muted-foreground">
              {query || category ? t("transactions.empty_filtered") : t("transactions.empty")}
            </p>
          )}
          <div className="border-t border-line pt-4">
            <Ledger>
              {groupByDay(visible).map(([day, rows]) => [
                <LedgerDate key={day}>{formatDay(day)}</LedgerDate>,
                ...rows.map((transaction) => (
                  <LedgerRow
                    key={transaction.id}
                    transaction={transaction}
                    onSelect={setSelected}
                  />
                )),
              ])}
            </Ledger>
          </div>
        </Panel>
        <MonthSide
          transactions={transactions.data}
          category={category}
          onCategoryChange={setCategory}
        />
      </div>
      <CategoryDialog transaction={selected} onClose={() => setSelected(null)} />
    </>
  );
}

function filter(
  transactions: Transaction[],
  query: string,
  category: Category | null,
): Transaction[] {
  const needle = query.trim().toLocaleLowerCase();
  return transactions.filter(
    (transaction) =>
      (!category || transaction.category === category) &&
      (!needle ||
        `${transaction.counterparty} ${transaction.description}`
          .toLocaleLowerCase()
          .includes(needle)),
  );
}

function groupByDay(transactions: Transaction[]): [string, Transaction[]][] {
  const days = new Map<string, Transaction[]>();
  for (const transaction of transactions) {
    days.set(transaction.date, [...(days.get(transaction.date) ?? []), transaction]);
  }
  return [...days.entries()];
}