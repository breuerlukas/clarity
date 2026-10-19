import { useState, type FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { useMutation } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogTitle } from "@/components/ui/dialog";
import { Field } from "@/components/field";
import { Notice } from "@/components/notice";
import { Segmented } from "@/components/segmented";
import { useRefreshCash } from "@/hooks/use-cash";
import { useSpace } from "@/hooks/use-space";
import { CategoryField } from "@/pages/transactions/category-field";
import { api, ApiError, type CashEntryDraft, type Transaction } from "@/lib/api";
import { isoDate } from "@/lib/dates";

type Direction = "expense" | "income";

interface Form {
  direction: Direction;
  amount: string;
  date: string;
  counterparty: string;
  category: string;
  note: string;
}

const formOf = (entry: Transaction | null): Form =>
  entry
    ? {
        direction: entry.amount < 0 ? "expense" : "income",
        amount: String(Math.abs(entry.amount)).replace(".", ","),
        date: entry.date,
        counterparty: entry.counterparty,
        category: entry.category,
        note: entry.description,
      }
    : { direction: "expense", amount: "", date: isoDate(new Date()), counterparty: "", category: "GROCERIES", note: "" };

const draftOf = (form: Form): CashEntryDraft => {
  const amount = Math.round(Number(form.amount.replace(",", ".")) * 100) / 100;
  return {
    date: form.date,
    amount: form.direction === "expense" ? -amount : amount,
    counterparty: form.counterparty.trim(),
    category: form.category,
    note: form.note.trim(),
  };
};

/** Books cash spent or received, or changes or deletes such an entry. */
export function CashEntryDialog({
  open,
  entry,
  onOpenChange,
}: {
  open: boolean;
  entry: Transaction | null;
  onOpenChange: (open: boolean) => void;
}) {
  const { t } = useTranslation();
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent closeLabel={t("common.cancel")} className="sm:max-w-xl">
        {open && <CashEntryForm key={entry?.id ?? "new"} entry={entry} onDone={() => onOpenChange(false)} />}
      </DialogContent>
    </Dialog>
  );
}

function CashEntryForm({ entry, onDone }: { entry: Transaction | null; onDone: () => void }) {
  const { t } = useTranslation();
  const refresh = useRefreshCash();
  const space = useSpace();
  const [form, setForm] = useState<Form>(() => formOf(entry));
  const [confirmDelete, setConfirmDelete] = useState(false);
  const done = async () => {
    await refresh();
    onDone();
  };
  const save = useMutation({
    mutationFn: () => (entry ? api.updateCashEntry(entry.id, draftOf(form)) : api.createCashEntry(space.id, draftOf(form))),
    onSuccess: done,
  });
  const remove = useMutation({ mutationFn: () => api.deleteCashEntry(entry!.id), onSuccess: done });
  const set = (key: keyof Form) => (value: string) => setForm((previous) => ({ ...previous, [key]: value }));
  const submit = (event: FormEvent) => {
    event.preventDefault();
    save.mutate();
  };
  const error = save.error ?? remove.error;
  return (
    <>
      <div className="grid gap-2">
        <DialogTitle>{t(entry ? "cash.edit_entry" : "cash.add_entry")}</DialogTitle>
        <DialogDescription>{t("cash.entry_hint")}</DialogDescription>
      </div>
      <form onSubmit={submit} className="grid gap-4">
        <Segmented<Direction>
          label={t("cash.direction")}
          value={form.direction}
          options={[
            { value: "expense", label: t("cash.expense") },
            { value: "income", label: t("cash.income") },
          ]}
          onChange={(direction) =>
            setForm((previous) => ({
              ...previous,
              direction,
              category: entry ? previous.category : direction === "expense" ? "GROCERIES" : "INCOME",
            }))
          }
        />
        <div className="grid gap-4 sm:grid-cols-2">
          <Field
            label={t("cash.amount")}
            value={form.amount}
            onChange={(event) => set("amount")(event.target.value)}
            inputMode="decimal"
            pattern="[0-9]+([.,][0-9]{1,2})?"
            required
            autoFocus
          />
          <Field label={t("cash.date")} type="date" value={form.date} onChange={(event) => set("date")(event.target.value)} required />
        </div>
        <Field
          label={t("cash.counterparty")}
          value={form.counterparty}
          onChange={(event) => set("counterparty")(event.target.value)}
          maxLength={120}
          autoComplete="off"
        />
        <CategoryField
          label={t("cash.category")}
          value={form.category}
          kind={form.direction === "expense" ? "SPENDING" : "INCOME"}
          onChange={set("category")}
        />
        <Field label={t("cash.note")} value={form.note} onChange={(event) => set("note")(event.target.value)} maxLength={500} />
        {error && (
          <Notice>
            {error instanceof ApiError && error.code && t(`cash.errors.${error.code}`, { defaultValue: "" })
              ? t(`cash.errors.${error.code}`)
              : t("common.error")}
          </Notice>
        )}
        <div className="flex flex-wrap items-center justify-end gap-2">
          {entry && (
            <Button
              type="button"
              variant={confirmDelete ? "destructive" : "link"}
              className={confirmDelete ? "mr-auto" : "mr-auto px-0 text-muted-foreground"}
              disabled={remove.isPending}
              onClick={() => (confirmDelete ? remove.mutate() : setConfirmDelete(true))}
            >
              {t(confirmDelete ? "cash.delete_confirm" : "cash.delete")}
            </Button>
          )}
          <DialogClose asChild>
            <Button type="button" variant="outline">
              {t("common.cancel")}
            </Button>
          </DialogClose>
          <Button type="submit" loading={save.isPending}>
            {t("common.save")}
          </Button>
        </div>
      </form>
    </>
  );
}
