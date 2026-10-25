import { useState, type FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { useMutation } from "@tanstack/react-query";
import { Plus, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { DialogDescription, DialogTitle } from "@/components/ui/dialog";
import { Field } from "@/components/field";
import { ConfirmDelete } from "@/components/confirm-delete";
import { Notice } from "@/components/notice";
import { api, ApiError, receiptFileUrl, type Receipt, type ReceiptDraft } from "@/lib/api";

interface ItemRow {
  name: string;
  amount: string;
}

const decimal = (value: number) => value.toFixed(2).replace(".", ",");
const parsed = (value: string) => Math.round(Number(value.replace(",", ".")) * 100) / 100;

/**
 * What was read from a receipt, every field open to correction, beside the
 * photo. Saving moves on to assigning it; a wrong photo can be discarded.
 */
export function ReceiptReview({
  receipt,
  preview,
  onSaved,
  onDiscarded,
}: {
  receipt: Receipt;
  /** The photo as just taken; otherwise it loads from the server. */
  preview: string | null;
  onSaved: (receipt: Receipt) => void;
  onDiscarded: () => void;
}) {
  const { t } = useTranslation();
  const [merchant, setMerchant] = useState(receipt.merchant);
  const [date, setDate] = useState(receipt.date ?? "");
  const [total, setTotal] = useState(receipt.total === null ? "" : decimal(receipt.total));
  const [items, setItems] = useState<ItemRow[]>(
    receipt.items.map((item) => ({ name: item.name, amount: decimal(item.amount) })),
  );
  const draft = (): ReceiptDraft => ({
    merchant: merchant.trim(),
    date,
    total: total ? String(parsed(total)) : "",
    items: items
      .filter((item) => item.name.trim() && item.amount)
      .map((item) => ({ name: item.name.trim(), amount: parsed(item.amount) })),
  });
  const save = useMutation({
    mutationFn: () => api.updateReceipt(receipt.id, draft()),
    onSuccess: ({ receipt: saved }) => onSaved(saved),
  });
  const discard = useMutation({ mutationFn: () => api.deleteReceipt(receipt.id), onSuccess: onDiscarded });
  const setItem = (index: number, change: Partial<ItemRow>) =>
    setItems((previous) => previous.map((item, position) => (position === index ? { ...item, ...change } : item)));
  const submit = (event: FormEvent) => {
    event.preventDefault();
    save.mutate();
  };
  const error = save.error ?? discard.error;
  const nothingRead = !receipt.merchant && receipt.total === null;
  return (
    <>
      <div className="grid gap-2">
        <DialogTitle>{t("receipts.review")}</DialogTitle>
        <DialogDescription>{t("receipts.review_hint")}</DialogDescription>
      </div>
      <ReceiptPreview receipt={receipt} preview={preview} />
      {nothingRead && <Notice>{t("receipts.read_nothing")}</Notice>}
      <form onSubmit={submit} className="grid gap-4">
        <Field label={t("receipts.merchant")} value={merchant} onChange={(event) => setMerchant(event.target.value)} maxLength={120} />
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label={t("receipts.date")} type="date" value={date} onChange={(event) => setDate(event.target.value)} required />
          <Field
            label={t("receipts.total")}
            value={total}
            onChange={(event) => setTotal(event.target.value)}
            inputMode="decimal"
            pattern="[0-9]+([.,][0-9]{1,2})?"
            required
          />
        </div>
        <fieldset className="grid gap-2">
          <legend className="mb-2 text-sm font-bold text-label">{t("receipts.items")}</legend>
          {items.map((item, index) => (
            <div key={index} className="grid grid-cols-[1fr_7rem_auto] gap-2">
              <Input
                aria-label={t("receipts.item_name")}
                value={item.name}
                onChange={(event) => setItem(index, { name: event.target.value })}
                maxLength={120}
              />
              <Input
                aria-label={t("receipts.item_amount")}
                value={item.amount}
                onChange={(event) => setItem(index, { amount: event.target.value })}
                inputMode="decimal"
                pattern="-?[0-9]+([.,][0-9]{1,2})?"
              />
              <Button
                type="button"
                variant="ghost"
                size="icon"
                aria-label={t("receipts.remove_item")}
                onClick={() => setItems((previous) => previous.filter((_, position) => position !== index))}
              >
                <X />
              </Button>
            </div>
          ))}
          <Button
            type="button"
            variant="link"
            className="justify-self-start px-0"
            onClick={() => setItems((previous) => [...previous, { name: "", amount: "" }])}
          >
            <Plus />
            {t("receipts.add_item")}
          </Button>
        </fieldset>
        {error && (
          <Notice>
            {error instanceof ApiError && error.code && t(`receipts.errors.${error.code}`, { defaultValue: "" })
              ? t(`receipts.errors.${error.code}`)
              : t("common.error")}
          </Notice>
        )}
        <div className="flex flex-wrap items-center justify-end gap-2">
          <ConfirmDelete
            label={t("receipts.discard")}
            name={receipt.merchant}
            consequence={t("receipts.discard_consequence")}
            pending={discard.isPending}
            error={discard.error}
            errors="receipts"
            onConfirm={() => discard.mutate()}
          />
          <Button type="submit" loading={save.isPending}>
            {t("receipts.continue")}
          </Button>
        </div>
      </form>
    </>
  );
}

/** The photo, or a link to the PDF; a photo the server cannot find is left out. */
function ReceiptPreview({ receipt, preview }: { receipt: Receipt; preview: string | null }) {
  const { t } = useTranslation();
  const [failed, setFailed] = useState(false);
  if (receipt.file.content_type === "application/pdf") {
    return (
      <a href={receiptFileUrl(receipt.id)} className="text-sm font-bold text-brand-text hover:underline">
        {t("receipts.open_file", { name: receipt.file.name })}
      </a>
    );
  }
  if (failed) {
    return null;
  }
  return (
    <img
      src={preview ?? receiptFileUrl(receipt.id)}
      alt={t("receipts.photo")}
      onError={() => setFailed(true)}
      className="max-h-72 justify-self-center rounded-2xl object-contain ring-1 ring-divider"
    />
  );
}
