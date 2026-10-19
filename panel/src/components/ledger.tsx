import type { ReactNode } from "react";
import { useTranslation } from "react-i18next";
import { Banknote, ReceiptText, Split } from "lucide-react";
import { AmountBadge } from "@/components/amount-badge";
import { CategoryIcon } from "@/components/category-icon";
import { useCategoryLook } from "@/hooks/use-categories";
import { Skeleton } from "@/components/ui/skeleton";
import type { Transaction } from "@/lib/api";
import { isCounted } from "@/lib/statistics";
import { cn } from "@/lib/utils";

export function Ledger({
  children,
  className,
}: {
  children: ReactNode;
  className?: string;
}) {
  return <ul className={cn("zebra -mx-2", className)}>{children}</ul>;
}

/** Stand-in rows while the data loads, shaped like a {@link LedgerRow}. */
export function LedgerSkeleton({ rows }: { rows: number }) {
  return (
    <ul className="-mx-2" aria-busy="true">
      {Array.from({ length: rows }, (_, index) => (
        <li key={index} className="grid grid-cols-[auto_1fr_auto] items-center gap-3 px-2 py-2">
          <Skeleton className="size-9 rounded-full" />
          <span className="grid gap-1.5">
            <Skeleton className="h-4 w-2/5" />
            <Skeleton className="h-3.5 w-1/4" />
          </span>
          <Skeleton className="h-6 w-20 rounded-full" />
        </li>
      ))}
    </ul>
  );
}

/**
 * One day with its rows. The date sticks to the top while its rows scroll by
 * and is pushed away by the next day, because each day is its own section.
 */
export function LedgerDay({ label, children }: { label: string; children: ReactNode }) {
  return (
    <li>
      <div className="sticky top-0 z-10 bg-card px-2 pt-3 pb-1.5 text-sm font-bold text-label">
        {label}
      </div>
      <ul className="zebra">{children}</ul>
    </li>
  );
}

export function LedgerRow({
  transaction,
  cash = false,
  onSelect,
}: {
  transaction: Transaction;
  /** An entry of the cash wallet; without a counterparty it is a count. */
  cash?: boolean;
  onSelect?: (transaction: Transaction) => void;
}) {
  const { t } = useTranslation();
  const look = useCategoryLook();
  const content = (
    <>
      <CategoryIcon category={transaction.category} />
      <span className="grid min-w-0 gap-0.5">
        <span className="truncate font-medium">
          {transaction.counterparty || t(cash ? "cash.correction" : "transactions.no_counterparty")}
        </span>
        <span className="flex min-w-0 items-center gap-1.5 text-sm text-muted-foreground">
          <span className="truncate">{look(transaction.category).label}</span>
          {cash && <Banknote className="size-3.5 shrink-0" aria-label={t("cash.title")} />}
          {transaction.receipt && <ReceiptText className="size-3.5 shrink-0" aria-label={t("receipts.attached")} />}
          {Boolean(transaction.splits?.length) && <Split className="size-3.5 shrink-0" aria-label={t("transactions.split")} />}
        </span>
      </span>
      <AmountBadge
        value={transaction.amount}
        currency={transaction.currency}
        neutral={!isCounted(transaction)}
      />
    </>
  );
  const rowClass =
    "grid w-full grid-cols-[auto_1fr_auto] items-center gap-3 rounded-md px-2 py-2 text-left";
  return (
    <li className="zebra-row rounded-md">
      {onSelect ? (
        <button
          type="button"
          onClick={() => onSelect(transaction)}
          className={cn(rowClass, "hover:bg-raised")}
        >
          {content}
        </button>
      ) : (
        <div className={rowClass}>{content}</div>
      )}
    </li>
  );
}