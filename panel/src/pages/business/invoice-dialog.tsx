import { useId, useState, type FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Download, Link2, Unlink } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from "@/components/ui/dialog";
import { Field } from "@/components/field";
import { Notice } from "@/components/notice";
import { usePaymentSuggestions } from "@/hooks/use-business";
import {
  api,
  invoiceDocumentUrl,
  type Invoice,
  type InvoiceDirection,
  type InvoiceDraft,
} from "@/lib/api";
import { formatDate, formatMoney } from "@/lib/format";
import { counterpartyOf, invoiceError, isOpen } from "@/lib/invoices";
import { InvoiceStatusBadge, ReviewBadge } from "@/pages/business/invoice-status";

const DIRECTIONS: InvoiceDirection[] = ["OUTGOING", "INCOMING"];

const text = (value: number | null) => (value === null ? "" : String(value));

const draftOf = (invoice: Invoice): InvoiceDraft => ({
  direction: invoice.direction,
  number: invoice.number,
  issuer: invoice.issuer,
  recipient: invoice.recipient,
  iban: invoice.iban ?? "",
  currency: invoice.currency,
  net_amount: text(invoice.net_amount),
  tax_amount: text(invoice.tax_amount),
  gross_amount: text(invoice.gross_amount),
  invoice_date: invoice.invoice_date,
  due_date: invoice.due_date ?? "",
});

const AMOUNT = "-?[0-9]+([.,][0-9]{1,2})?";
const amount = (value: string) => value.trim().replace(",", ".");

/**
 * One invoice: its fields to check and correct, where it came from, the
 * bookings that paid it and the ones that might, and the ways to close it
 * by hand.
 */
export function InvoiceDialog({ invoice, onClose }: { invoice: Invoice | null; onClose: () => void }) {
  const { t } = useTranslation();
  return (
    <Dialog open={invoice !== null} onOpenChange={(open) => !open && onClose()}>
      {invoice && (
        <DialogContent closeLabel={t("common.close")} className="gap-6 sm:max-w-2xl">
          <InvoiceDetails key={invoice.id} invoice={invoice} onClose={onClose} />
        </DialogContent>
      )}
    </Dialog>
  );
}

function InvoiceDetails({ invoice, onClose }: { invoice: Invoice; onClose: () => void }) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const refresh = async () => {
    await queryClient.invalidateQueries({ queryKey: ["invoices"] });
    await queryClient.invalidateQueries({ queryKey: ["payment-suggestions"] });
    await queryClient.invalidateQueries({ queryKey: ["business-transactions"] });
  };
  const settle = useMutation({
    mutationFn: () => api.settleInvoice(invoice.id, !invoice.settled),
    onSuccess: refresh,
  });
  const [confirmDelete, setConfirmDelete] = useState(false);
  const remove = useMutation({
    mutationFn: () => api.deleteInvoice(invoice.id),
    onSuccess: async () => {
      await refresh();
      onClose();
    },
  });
  const error = settle.error ?? remove.error;
  return (
    <>
      <div className="flex items-center gap-4 pr-8">
        <div className="min-w-0 flex-1">
          <DialogTitle className="truncate pr-0">
            {counterpartyOf(invoice) || t("invoices.no_counterparty")}
          </DialogTitle>
          <DialogDescription className="flex flex-wrap items-center gap-2">
            <span>{invoice.number || t("invoices.no_number")}</span>
            <InvoiceStatusBadge status={invoice.status} />
            {invoice.needs_review && <ReviewBadge />}
          </DialogDescription>
        </div>
        <span className="figures shrink-0 text-2xl font-extrabold tracking-[-0.02em]">
          {formatMoney(invoice.gross_amount, invoice.currency)}
        </span>
      </div>
      {invoice.needs_review && <Notice>{t(`invoices.review_hint_${invoice.format}`)}</Notice>}
      <Source invoice={invoice} />
      <Payments invoice={invoice} onChange={refresh} />
      <InvoiceForm invoice={invoice} onSaved={refresh} />
      {error && <Notice>{invoiceError(t, error)}</Notice>}
      <div className="flex flex-wrap items-center gap-2 border-t pt-5">
        <Button
          type="button"
          variant={confirmDelete ? "destructive" : "link"}
          className={confirmDelete ? "mr-auto" : "mr-auto px-0 text-muted-foreground"}
          disabled={remove.isPending}
          onClick={() => (confirmDelete ? remove.mutate() : setConfirmDelete(true))}
        >
          {t(confirmDelete ? "invoices.delete_confirm" : "invoices.delete")}
        </Button>
        {(invoice.settled || isOpen(invoice)) && (
          <Button variant="outline" disabled={settle.isPending} onClick={() => settle.mutate()}>
            {t(invoice.settled ? "invoices.unsettle" : "invoices.settle")}
          </Button>
        )}
      </div>
    </>
  );
}

/** The mail it came in, the original file and how it was read. */
function Source({ invoice }: { invoice: Invoice }) {
  const { t } = useTranslation();
  return (
    <dl className="grid gap-3 rounded-2xl bg-raised p-4 text-sm sm:grid-cols-[8rem_1fr]">
      {invoice.mail && (
        <>
          <dt className="text-muted-foreground">{t("invoices.mail")}</dt>
          <dd className="grid min-w-0 gap-0.5">
            <span className="break-words">{invoice.mail.subject || "–"}</span>
            <span className="text-muted-foreground break-words">
              {invoice.mail.sender} · {formatDate(invoice.mail.received_at)}
            </span>
          </dd>
        </>
      )}
      <dt className="text-muted-foreground">{t("invoices.format")}</dt>
      <dd>{t(`invoices.format_${invoice.format}`)}</dd>
      {invoice.announced_at && (
        <>
          <dt className="text-muted-foreground">{t("invoices.announced")}</dt>
          <dd>
            {invoice.announced_amount !== null
              ? t("invoices.announced_with_amount", {
                  date: formatDate(invoice.announced_at),
                  amount: formatMoney(invoice.announced_amount, invoice.currency),
                })
              : formatDate(invoice.announced_at)}
          </dd>
        </>
      )}
      {invoice.document && (
        <>
          <dt className="text-muted-foreground">{t("invoices.document")}</dt>
          <dd className="min-w-0">
            <a
              href={invoiceDocumentUrl(invoice.id)}
              download={invoice.document.name}
              className="inline-flex max-w-full items-center gap-1.5 font-bold text-brand-text hover:underline"
            >
              <Download className="size-4 shrink-0" aria-hidden />
              <span className="truncate">{invoice.document.name}</span>
            </a>
          </dd>
        </>
      )}
    </dl>
  );
}

/** The bookings that paid it, and while something is open, the ones that might. */
function Payments({ invoice, onChange }: { invoice: Invoice; onChange: () => Promise<void> }) {
  const { t } = useTranslation();
  const suggestions = usePaymentSuggestions(isOpen(invoice) ? invoice.id : null);
  const link = useMutation({
    mutationFn: (transaction: string) => api.linkPayment(invoice.id, transaction),
    onSuccess: onChange,
  });
  const unlink = useMutation({
    mutationFn: (payment: string) => api.unlinkPayment(payment),
    onSuccess: onChange,
  });
  const error = link.error ?? unlink.error;
  return (
    <section className="grid gap-3">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h3 className="text-sm font-bold text-label">{t("invoices.payments")}</h3>
        <span className="figures text-sm text-muted-foreground">
          {t("invoices.paid_of", {
            paid: formatMoney(invoice.paid_amount, invoice.currency),
            gross: formatMoney(invoice.gross_amount, invoice.currency),
          })}
        </span>
      </div>
      {invoice.payments.length === 0 && <p className="text-sm text-muted-foreground">{t("invoices.no_payments")}</p>}
      <ul className="grid gap-1">
        {invoice.payments.map((payment) => (
          <li key={payment.id} className="flex items-center gap-3 rounded-xl px-3 py-2 ring-1 ring-divider">
            <span className="grid min-w-0 flex-1">
              <span className="figures font-bold">{formatMoney(payment.amount, invoice.currency)}</span>
              <span className="text-xs text-muted-foreground">
                {formatDate(payment.date)} · {t(payment.automatic ? "invoices.linked_automatic" : "invoices.linked_manual")}
              </span>
            </span>
            <Button
              size="sm"
              variant="ghost"
              disabled={unlink.isPending}
              onClick={() => unlink.mutate(payment.id)}
            >
              <Unlink />
              {t("invoices.unlink")}
            </Button>
          </li>
        ))}
      </ul>
      {isOpen(invoice) && (suggestions.data?.length ?? 0) > 0 && (
        <>
          <h3 className="mt-2 text-sm font-bold text-label">{t("invoices.suggestions")}</h3>
          <ul className="grid max-h-64 gap-1 overflow-y-auto">
            {suggestions.data!.map(({ transaction, reasons }) => (
              <li key={transaction.id} className="flex items-center gap-3 rounded-xl px-3 py-2 ring-1 ring-divider">
                <span className="grid min-w-0 flex-1 gap-0.5">
                  <span className="flex min-w-0 items-center gap-2">
                    <span className="truncate font-medium">
                      {transaction.counterparty || t("transactions.no_counterparty")}
                    </span>
                    <span className="figures shrink-0 text-sm font-bold">
                      {formatMoney(transaction.amount, transaction.currency, { signed: true })}
                    </span>
                  </span>
                  <span className="truncate text-xs text-muted-foreground">
                    {formatDate(transaction.date)} · {transaction.description || "–"}
                  </span>
                  <span className="flex flex-wrap gap-1">
                    {reasons.map((reason) => (
                      <span key={reason} className="rounded-full bg-brand/12 px-2 py-px text-[11px] font-bold text-brand-text">
                        {t(`invoices.reason_${reason}`)}
                      </span>
                    ))}
                  </span>
                </span>
                <Button size="sm" variant="outline" disabled={link.isPending} onClick={() => link.mutate(transaction.id)}>
                  <Link2 />
                  {t("invoices.link")}
                </Button>
              </li>
            ))}
          </ul>
        </>
      )}
      {error && <Notice className="text-sm">{invoiceError(t, error)}</Notice>}
    </section>
  );
}

/**
 * The fields as read, to correct. Saving confirms them, so an invoice that
 * needed a look no longer does, and matches its payments again.
 */
function InvoiceForm({ invoice, onSaved }: { invoice: Invoice; onSaved: () => Promise<void> }) {
  const { t } = useTranslation();
  const directionId = useId();
  const [form, setForm] = useState<InvoiceDraft>(() => draftOf(invoice));
  const [editing, setEditing] = useState(invoice.needs_review);
  const save = useMutation({
    mutationFn: () =>
      api.updateInvoice(invoice.id, {
        ...form,
        iban: form.iban.replace(/\s/g, "").toUpperCase(),
        currency: form.currency.trim().toUpperCase(),
        net_amount: amount(form.net_amount),
        tax_amount: amount(form.tax_amount),
        gross_amount: amount(form.gross_amount),
      }),
    onSuccess: async () => {
      await onSaved();
      setEditing(false);
    },
  });
  const set = (key: keyof InvoiceDraft) => (value: string) => setForm((previous) => ({ ...previous, [key]: value }));
  const submit = (event: FormEvent) => {
    event.preventDefault();
    save.mutate();
  };
  if (!editing) {
    return (
      <section className="grid gap-3">
        <div className="flex items-center justify-between gap-2">
          <h3 className="text-sm font-bold text-label">{t("invoices.details")}</h3>
          <Button size="sm" variant="outline" onClick={() => setEditing(true)}>
            {t("invoices.edit")}
          </Button>
        </div>
        <dl className="grid gap-x-4 gap-y-2 text-sm sm:grid-cols-[8rem_1fr]">
          <dt className="text-muted-foreground">{t("invoices.issuer")}</dt>
          <dd className="break-words">{invoice.issuer || "–"}</dd>
          <dt className="text-muted-foreground">{t("invoices.recipient")}</dt>
          <dd className="break-words">{invoice.recipient || "–"}</dd>
          <dt className="text-muted-foreground">{t("invoices.invoice_date")}</dt>
          <dd>{formatDate(invoice.invoice_date)}</dd>
          <dt className="text-muted-foreground">{t("invoices.due_date")}</dt>
          <dd>{invoice.due_date ? formatDate(invoice.due_date) : "–"}</dd>
          <dt className="text-muted-foreground">{t("invoices.amounts")}</dt>
          <dd className="figures">
            {[
              invoice.net_amount !== null && `${t("invoices.net")} ${formatMoney(invoice.net_amount, invoice.currency)}`,
              invoice.tax_amount !== null && `${t("invoices.tax")} ${formatMoney(invoice.tax_amount, invoice.currency)}`,
              `${t("invoices.gross")} ${formatMoney(invoice.gross_amount, invoice.currency)}`,
            ]
              .filter(Boolean)
              .join(" · ")}
          </dd>
          <dt className="text-muted-foreground">{t("invoices.iban")}</dt>
          <dd className="break-all">{invoice.iban || "–"}</dd>
        </dl>
      </section>
    );
  }
  return (
    <form onSubmit={submit} className="grid gap-4">
      <h3 className="text-sm font-bold text-label">{t("invoices.details")}</h3>
      <div className="grid gap-4 sm:grid-cols-2">
        <div className="grid content-start gap-2">
          <Label htmlFor={directionId}>{t("invoices.direction")}</Label>
          <select
            id={directionId}
            value={form.direction}
            onChange={(event) => set("direction")(event.target.value)}
            className="h-12 rounded-2xl border-[1.5px] border-transparent bg-ground px-4 text-sm text-foreground outline-none focus-visible:border-primary"
          >
            {DIRECTIONS.map((direction) => (
              <option key={direction} value={direction}>
                {t(`invoices.direction_${direction}`)}
              </option>
            ))}
          </select>
        </div>
        <Field
          label={t("invoices.number")}
          value={form.number}
          onChange={(event) => set("number")(event.target.value)}
          maxLength={64}
          required
        />
        <Field
          label={t("invoices.issuer")}
          value={form.issuer}
          onChange={(event) => set("issuer")(event.target.value)}
          maxLength={160}
        />
        <Field
          label={t("invoices.recipient")}
          value={form.recipient}
          onChange={(event) => set("recipient")(event.target.value)}
          maxLength={160}
        />
        <Field
          label={t("invoices.invoice_date")}
          type="date"
          value={form.invoice_date}
          onChange={(event) => set("invoice_date")(event.target.value)}
          required
        />
        <Field
          label={t("invoices.due_date")}
          type="date"
          value={form.due_date}
          onChange={(event) => set("due_date")(event.target.value)}
        />
      </div>
      <div className="grid gap-4 sm:grid-cols-[1fr_1fr_1fr_6rem]">
        <Field
          label={t("invoices.net")}
          value={form.net_amount}
          onChange={(event) => set("net_amount")(event.target.value)}
          inputMode="decimal"
          pattern={AMOUNT}
        />
        <Field
          label={t("invoices.tax")}
          value={form.tax_amount}
          onChange={(event) => set("tax_amount")(event.target.value)}
          inputMode="decimal"
          pattern={AMOUNT}
        />
        <Field
          label={t("invoices.gross")}
          value={form.gross_amount}
          onChange={(event) => set("gross_amount")(event.target.value)}
          inputMode="decimal"
          pattern={AMOUNT}
          required
        />
        <Field
          label={t("invoices.currency")}
          value={form.currency}
          onChange={(event) => set("currency")(event.target.value)}
          maxLength={3}
          pattern="[A-Za-z]{3}"
          required
        />
      </div>
      <Field
        label={t("invoices.iban")}
        hint={t("invoices.iban_hint")}
        value={form.iban}
        onChange={(event) => set("iban")(event.target.value)}
        maxLength={42}
        autoComplete="off"
      />
      {save.error && <Notice>{invoiceError(t, save.error)}</Notice>}
      <div className="flex flex-wrap justify-end gap-2">
        {invoice.needs_review ? (
          <DialogClose asChild>
            <Button type="button" variant="outline">
              {t("common.cancel")}
            </Button>
          </DialogClose>
        ) : (
          <Button
            type="button"
            variant="outline"
            onClick={() => {
              setForm(draftOf(invoice));
              setEditing(false);
            }}
          >
            {t("common.cancel")}
          </Button>
        )}
        <Button type="submit" disabled={save.isPending}>
          {t(invoice.needs_review ? "invoices.confirm" : "common.save")}
        </Button>
      </div>
    </form>
  );
}
