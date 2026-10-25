import { useId, useState, type FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { useMutation, useQueryClient } from "@tanstack/react-query";
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
import { ConfirmDelete } from "@/components/confirm-delete";
import { Notice } from "@/components/notice";
import { useSpace } from "@/hooks/use-space";
import { api, ApiError, type BillingInterval, type Contract, type ContractDraft } from "@/lib/api";
import { INTERVAL_MONTHS } from "@/lib/business";
import { isoDate } from "@/lib/dates";

const INTERVALS = Object.keys(INTERVAL_MONTHS) as BillingInterval[];

interface Form {
  customer: string;
  amount: string;
  interval: BillingInterval;
  start_date: string;
  term_months: string;
  notice_months: string;
}

const emptyForm = (): Form => ({
  customer: "",
  amount: "",
  interval: "MONTHLY",
  start_date: isoDate(new Date()),
  term_months: "12",
  notice_months: "3",
});

const formOf = (contract: Contract): Form => ({
  customer: contract.customer,
  amount: String(contract.amount),
  interval: contract.interval,
  start_date: contract.start_date,
  term_months: String(contract.term_months),
  notice_months: String(contract.notice_months),
});

const draftOf = (form: Form): ContractDraft => ({
  customer: form.customer.trim(),
  amount: Number(form.amount.replace(",", ".")),
  interval: form.interval,
  start_date: form.start_date,
  term_months: Number(form.term_months),
  notice_months: Number(form.notice_months),
});

/**
 * Creates a contract, or changes one when given. Existing customers are
 * offered while typing, so a second contract lands with the same customer.
 */
export function ContractDialog({
  open,
  contract,
  customers,
  onOpenChange,
}: {
  open: boolean;
  contract: Contract | null;
  customers: string[];
  onOpenChange: (open: boolean) => void;
}) {
  const { t } = useTranslation();
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent closeLabel={t("common.cancel")} className="sm:max-w-xl">
        {open && (
          <ContractForm
            key={contract?.id ?? "new"}
            contract={contract}
            customers={customers}
            onDone={() => onOpenChange(false)}
          />
        )}
      </DialogContent>
    </Dialog>
  );
}

function ContractForm({
  contract,
  customers,
  onDone,
}: {
  contract: Contract | null;
  customers: string[];
  onDone: () => void;
}) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const space = useSpace();
  const customerList = useId();
  const intervalId = useId();
  const [form, setForm] = useState<Form>(() => (contract ? formOf(contract) : emptyForm()));
  const done = async () => {
    await queryClient.invalidateQueries({ queryKey: ["contracts"] });
    await queryClient.invalidateQueries({ queryKey: ["business-transactions"] });
    onDone();
  };
  const save = useMutation({
    mutationFn: () =>
      contract ? api.updateContract(contract.id, draftOf(form)) : api.createContract(space.id, draftOf(form)),
    onSuccess: done,
  });
  const remove = useMutation({
    mutationFn: () => api.deleteContract(contract!.id),
    onSuccess: done,
  });
  const set = (key: keyof Form) => (value: string) => setForm((previous) => ({ ...previous, [key]: value }));
  const submit = (event: FormEvent) => {
    event.preventDefault();
    save.mutate();
  };
  const error = save.error ?? remove.error;
  return (
    <>
      <div className="grid gap-2">
        <DialogTitle>{t(contract ? "business.edit_contract" : "business.add_contract")}</DialogTitle>
        <DialogDescription>{t("business.contract_hint")}</DialogDescription>
      </div>
      <form onSubmit={submit} className="grid gap-4">
        <Field
          label={t("business.customer")}
          value={form.customer}
          onChange={(event) => set("customer")(event.target.value)}
          list={customerList}
          maxLength={120}
          required
          autoComplete="off"
        />
        <datalist id={customerList}>
          {customers.map((customer) => (
            <option key={customer} value={customer} />
          ))}
        </datalist>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field
            label={t("business.amount")}
            hint={t("business.amount_hint")}
            value={form.amount}
            onChange={(event) => set("amount")(event.target.value)}
            inputMode="decimal"
            pattern="[0-9]+([.,][0-9]{1,2})?"
            required
          />
          <div className="grid content-start gap-2">
            <Label htmlFor={intervalId}>{t("business.interval")}</Label>
            <select
              id={intervalId}
              value={form.interval}
              onChange={(event) => set("interval")(event.target.value)}
              className="h-12 rounded-2xl border-[1.5px] border-transparent bg-ground px-4 text-sm text-foreground outline-none focus-visible:border-primary"
            >
              {INTERVALS.map((interval) => (
                <option key={interval} value={interval}>
                  {t(`interval.${interval}`)}
                </option>
              ))}
            </select>
          </div>
        </div>
        <div className="grid gap-4 sm:grid-cols-3">
          <Field
            label={t("business.start_date")}
            type="date"
            value={form.start_date}
            onChange={(event) => set("start_date")(event.target.value)}
            required
          />
          <Field
            label={t("business.term")}
            hint={t("business.in_months")}
            type="number"
            min={1}
            max={120}
            value={form.term_months}
            onChange={(event) => set("term_months")(event.target.value)}
            required
          />
          <Field
            label={t("business.notice")}
            hint={t("business.in_months")}
            type="number"
            min={0}
            max={Number(form.term_months) || 120}
            value={form.notice_months}
            onChange={(event) => set("notice_months")(event.target.value)}
            required
          />
        </div>
        {error && (
          <Notice>
            {error instanceof ApiError && error.code && t(`business.errors.${error.code}`, { defaultValue: "" })
              ? t(`business.errors.${error.code}`)
              : t("common.error")}
          </Notice>
        )}
        <div className="flex flex-wrap items-center justify-end gap-2">
          {contract && (
            <ConfirmDelete
              label={t("business.delete")}
              name={contract.customer}
              consequence={t("business.delete_consequence")}
              pending={remove.isPending}
              error={remove.error}
              errors="business"
              onConfirm={() => remove.mutate()}
            />
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
