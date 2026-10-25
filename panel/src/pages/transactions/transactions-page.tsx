import { useEffect, useMemo, useRef, useState } from "react";
import { useSearchParams } from "react-router";
import { useTranslation } from "react-i18next";
import { Plus, ScanLine, Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { PageHeader, Panel } from "@/components/panel";
import { PeriodPicker } from "@/components/period-picker";
import { Notice } from "@/components/notice";
import { Ledger, LedgerDay, LedgerRow, LedgerSkeleton } from "@/components/ledger";
import { useCashWallet } from "@/hooks/use-cash";
import { useTransactionsBetween } from "@/hooks/use-finances";
import { usePeriodParam } from "@/hooks/use-period-param";
import { CashEntryDialog } from "@/pages/transactions/cash-entry-dialog";
import { CategoryDialog } from "@/pages/transactions/category-dialog";
import { CategorySelect } from "@/pages/transactions/category-select";
import { ExportButton } from "@/pages/transactions/export-button";
import { OpenReceipts } from "@/pages/transactions/open-receipts";
import { PeriodSummary } from "@/pages/transactions/period-summary";
import { ReceiptDialog } from "@/pages/transactions/receipt-dialog";
import type { Category, Receipt, Transaction } from "@/lib/api";
import { formatDay } from "@/lib/format";
import { periodRange } from "@/lib/period";

const PAGE_SIZE = 50;

export function TransactionsPage() {
  const { t } = useTranslation();
  const [period, setPeriod] = usePeriodParam("month");
  const [params, setParams] = useSearchParams();
  const [query, setQuery] = useState("");
  const [selected, setSelected] = useState<Transaction | null>(null);
  const [cashEntry, setCashEntry] = useState<{ entry: Transaction | null } | null>(null);
  const [scanning, setScanning] = useState<{ receipt: Receipt | null } | null>(null);
  const wallet = useCashWallet();
  const isCash = (transaction: Transaction) => transaction.account === wallet.data?.id;
  const open = (transaction: Transaction) =>
    isCash(transaction) ? setCashEntry({ entry: transaction }) : setSelected(transaction);
  const category = params.get("category") as Category | null;
  const { from, to } = periodRange(period);
  const transactions = useTransactionsBetween(from, to);
  const visible = useMemo(
    () => filter(transactions.data ?? [], query, category),
    [transactions.data, query, category],
  );
  // A new period or filter starts again at the first page.
  const filterKey = `${period.key}|${query}|${category ?? ""}`;
  const [paging, setPaging] = useState({ key: filterKey, limit: PAGE_SIZE });
  const limit = paging.key === filterKey ? paging.limit : PAGE_SIZE;
  const shown = visible.slice(0, limit);
  const end = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) =>
        entry.isIntersecting &&
        setPaging((current) => ({
          key: filterKey,
          limit: (current.key === filterKey ? current.limit : PAGE_SIZE) + PAGE_SIZE,
        })),
      { rootMargin: "400px" },
    );
    if (end.current) {
      observer.observe(end.current);
    }
    return () => observer.disconnect();
  }, [filterKey, shown.length, visible.length]);
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
      <PageHeader title={t("transactions.title")}>
        <PeriodPicker period={period} kinds={["month", "quarter", "year", "rolling"]} onChange={setPeriod} />
        <ExportButton transactions={visible} category={category} fileName={`clarity-${from}-${to}.csv`} />
        <Button variant="outline" onClick={() => setCashEntry({ entry: null })}>
          <Plus />
          {t("cash.add_entry")}
        </Button>
        <Button onClick={() => setScanning({ receipt: null })}>
          <ScanLine />
          {t("receipts.scan")}
        </Button>
      </PageHeader>
      <div className="grid items-start gap-6 2xl:grid-cols-[minmax(0,1fr)_400px]">
        <Panel className="gap-4 lg:h-[calc(100dvh-13.5rem)] lg:min-h-[32rem]">
          <div className="flex flex-wrap items-center gap-3">
            <label className="relative min-w-0 flex-1 sm:max-w-sm">
              <span className="sr-only">{t("transactions.search")}</span>
              <Search className="pointer-events-none absolute top-1/2 left-4 size-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                type="search"
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder={t("transactions.search_placeholder")}
                className="pl-10"
              />
            </label>
            <CategorySelect value={category} onChange={setCategory} />
            {transactions.data && (
              <span className="ml-auto text-sm text-muted-foreground">
                {t("transactions.count", { count: visible.length })}
              </span>
            )}
          </div>
          {transactions.isError && <Notice>{t("common.error")}</Notice>}
          {transactions.data && visible.length === 0 && (
            <p className="py-6 text-muted-foreground">
              {query || category ? t("transactions.empty_filtered") : t("transactions.empty")}
            </p>
          )}
          <div className="-mx-6 min-h-0 flex-1 border-t border-line px-6 lg:overflow-y-auto">
            {transactions.isPending && <LedgerSkeleton rows={10} />}
            <Ledger>
              {groupByDay(shown).map(([day, rows]) => (
                <LedgerDay key={day} label={formatDay(day)}>
                  {rows.map((transaction) => (
                    <LedgerRow key={transaction.id} transaction={transaction} cash={isCash(transaction)} onSelect={open} />
                  ))}
                </LedgerDay>
              ))}
            </Ledger>
            {visible.length > shown.length && <div key={shown.length} ref={end} className="h-px" />}
          </div>
        </Panel>
        <div className="grid gap-6">
          <OpenReceipts onOpen={(receipt) => setScanning({ receipt })} />
          <PeriodSummary transactions={transactions.data} category={category} onCategoryChange={setCategory} />
        </div>
      </div>
      <CategoryDialog transaction={selected} onClose={() => setSelected(null)} />
      <CashEntryDialog
        open={cashEntry !== null}
        entry={cashEntry?.entry ?? null}
        onOpenChange={(isOpen) => !isOpen && setCashEntry(null)}
      />
      <ReceiptDialog
        open={scanning !== null}
        receipt={scanning?.receipt ?? null}
        onOpenChange={(isOpen) => !isOpen && setScanning(null)}
      />
    </>
  );
}

function filter(transactions: Transaction[], query: string, category: Category | null) {
  const needle = query.trim().toLocaleLowerCase();
  return transactions.filter(
    (transaction) =>
      (!category ||
        transaction.category === category ||
        Boolean(transaction.splits?.some((part) => part.category === category))) &&
      (!needle ||
        `${transaction.counterparty} ${transaction.description}`.toLocaleLowerCase().includes(needle)),
  );
}

function groupByDay(transactions: Transaction[]): [string, Transaction[]][] {
  const days = new Map<string, Transaction[]>();
  for (const transaction of transactions) {
    days.set(transaction.date, [...(days.get(transaction.date) ?? []), transaction]);
  }
  return [...days.entries()];
}
