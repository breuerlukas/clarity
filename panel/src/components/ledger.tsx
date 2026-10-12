import type { ReactNode } from "react";
import { useTranslation } from "react-i18next";
import { AmountBadge } from "@/components/amount-badge";
import { CategoryIcon } from "@/components/category-icon";
import type { Transaction } from "@/lib/api";
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
  onSelect,
}: {
  transaction: Transaction;
  onSelect?: (transaction: Transaction) => void;
}) {
  const { t } = useTranslation();
  const content = (
    <>
      <CategoryIcon category={transaction.category} />
      <span className="grid min-w-0 gap-0.5">
        <span className="truncate font-medium">
          {transaction.counterparty || t("transactions.no_counterparty")}
        </span>
        <span className="truncate text-sm text-muted-foreground">
          {t(`category.${transaction.category}`)}
        </span>
      </span>
      <AmountBadge
        value={transaction.amount}
        currency={transaction.currency}
        neutral={transaction.category === "TRANSFER"}
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