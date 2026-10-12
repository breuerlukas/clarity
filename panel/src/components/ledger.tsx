import type { ReactNode } from "react";
import { useTranslation } from "react-i18next";
import { Amount } from "@/components/amount";
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

export function LedgerDate({ children }: { children: ReactNode }) {
  return (
    <li className="px-2 pt-5 pb-1.5 text-sm font-medium text-muted-foreground first:pt-0">
      {children}
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
      <Amount
        value={transaction.amount}
        currency={transaction.currency}
        className="font-medium"
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