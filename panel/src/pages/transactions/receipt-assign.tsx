import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { Switch } from "@/components/ui/switch";
import { DialogDescription, DialogTitle } from "@/components/ui/dialog";
import { Notice } from "@/components/notice";
import { Segmented } from "@/components/segmented";
import { CategoryField } from "@/pages/transactions/category-field";
import { api, ApiError, type Category, type Receipt, type Transaction } from "@/lib/api";
import { formatDate, formatMoney } from "@/lib/format";
import { isSplit, splitFromItems } from "@/lib/receipts";
import { cn } from "@/lib/utils";

type Payment = "card" | "cash";

/**
 * Where a reviewed receipt goes: onto the card payment it belongs to, or
 * into the cash wallet as an expense. Either way the payment can be split
 * into categories along the receipt's items.
 */
export function ReceiptAssign({
  receipt,
  onBack,
  onAssigned,
}: {
  receipt: Receipt;
  onBack: () => void;
  onAssigned: () => void;
}) {
  const { t } = useTranslation();
  const suggestions = useQuery({
    queryKey: ["receipt-suggestions", receipt.id, receipt.date, receipt.total],
    queryFn: () => api.receiptSuggestions(receipt.id),
    select: (data) => data.suggestions,
  });
  const [chosenPayment, setPayment] = useState<Payment | null>(null);
  const payment = chosenPayment ?? (suggestions.data?.length === 0 ? "cash" : "card");
  const [chosenTransaction, setTransaction] = useState<Transaction | null>(null);
  const transaction = chosenTransaction ?? suggestions.data?.[0] ?? null;
  const [cashCategory, setCashCategory] = useState<Category>("GROCERIES");
  const [split, setSplit] = useState(false);
  const [itemCategories, setItemCategories] = useState<Record<number, Category>>({});
  const mainCategory = payment === "card" ? (transaction?.category ?? "OTHER") : cashCategory;
  const amount = -(receipt.total ?? 0);
  const items = receipt.items.map((item, index) => ({ ...item, category: itemCategories[index] ?? mainCategory }));
  const parts = splitFromItems(amount, mainCategory, items);
  const splitFits = isSplit(parts, amount);
  const assign = useMutation({
    mutationFn: async () => {
      const assigned =
        payment === "card"
          ? await api.attachReceipt(receipt.id, transaction!.id)
          : await api.bookReceiptAsCash(receipt.id, cashCategory);
      if (split) {
        await api.splitTransaction(assigned.transaction.id, parts);
      }
    },
    onSuccess: onAssigned,
  });
  return (
    <>
      <div className="grid gap-2">
        <DialogTitle>{t("receipts.assign")}</DialogTitle>
        <DialogDescription>
          {[receipt.merchant, receipt.date && formatDate(receipt.date), receipt.total !== null && formatMoney(receipt.total)]
            .filter(Boolean)
            .join(" · ")}
        </DialogDescription>
      </div>
      <Segmented<Payment>
        label={t("receipts.paid_with")}
        value={payment}
        options={[
          { value: "card", label: t("receipts.paid_card") },
          { value: "cash", label: t("receipts.paid_cash") },
        ]}
        onChange={setPayment}
      />
      {payment === "card" ? (
        <fieldset className="grid gap-2">
          <legend className="mb-2 text-sm font-bold text-label">{t("receipts.suggestions")}</legend>
          {suggestions.data?.length === 0 && <p className="text-sm text-muted-foreground">{t("receipts.no_suggestions")}</p>}
          {suggestions.data?.map((suggestion) => (
            <button
              key={suggestion.id}
              type="button"
              aria-pressed={transaction?.id === suggestion.id}
              onClick={() => setTransaction(suggestion)}
              className={cn(
                "flex items-center justify-between gap-3 rounded-xl px-3 py-2.5 text-left text-sm ring-1 ring-divider hover:bg-raised",
                transaction?.id === suggestion.id && "bg-brand/12 ring-brand/40",
              )}
            >
              <span className="grid min-w-0">
                <span className="truncate font-bold">{suggestion.counterparty || t("transactions.no_counterparty")}</span>
                <span className="text-muted-foreground">{formatDate(suggestion.date)}</span>
              </span>
              <span className="figures font-bold">{formatMoney(suggestion.amount, suggestion.currency)}</span>
            </button>
          ))}
        </fieldset>
      ) : (
        <CategoryField label={t("receipts.category")} value={cashCategory} kind="SPENDING" onChange={setCashCategory} />
      )}
      {receipt.items.length > 0 && (
        <label className="flex items-center justify-between gap-4 rounded-2xl bg-raised px-4 py-3 text-sm">
          <span>{t("receipts.split")}</span>
          <Switch checked={split} onCheckedChange={setSplit} />
        </label>
      )}
      {split && (
        <div className="grid gap-3 sm:grid-cols-2">
          {items.map((item, index) => (
            <CategoryField
              key={index}
              label={`${item.name} · ${formatMoney(item.amount)}`}
              value={item.category}
              kind="SPENDING"
              onChange={(category) => setItemCategories((previous) => ({ ...previous, [index]: category }))}
            />
          ))}
        </div>
      )}
      {split && !splitFits && <Notice>{t("receipts.split_invalid")}</Notice>}
      {assign.isError && (
        <Notice>
          {assign.error instanceof ApiError && assign.error.code && t(`receipts.errors.${assign.error.code}`, { defaultValue: "" })
            ? t(`receipts.errors.${assign.error.code}`)
            : t("common.error")}
        </Notice>
      )}
      <div className="flex flex-wrap items-center justify-end gap-2">
        <Button type="button" variant="outline" className="mr-auto" onClick={onBack}>
          {t("receipts.back")}
        </Button>
        <Button
          loading={assign.isPending}
          disabled={(payment === "card" && !transaction) || (split && !splitFits)}
          onClick={() => assign.mutate()}
        >
          {t("receipts.save")}
        </Button>
      </div>
    </>
  );
}
