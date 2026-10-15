import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from "@/components/ui/dialog";
import { Notice } from "@/components/notice";
import { api, type BusinessTransaction, type Contract, type PaymentKind } from "@/lib/api";
import { contractStatus, matchLabel, type PaymentMatch } from "@/lib/business";
import { formatDate, formatMoney } from "@/lib/format";
import { cn } from "@/lib/utils";

/** "AUTO" or a kind, with the contract as "CONTRACT:<id>". */
type Choice = string;

const choiceOf = (transaction: BusinessTransaction): Choice =>
  !transaction.assignment
    ? "AUTO"
    : transaction.assignment.kind === "CONTRACT"
      ? `CONTRACT:${transaction.assignment.contract}`
      : transaction.assignment.kind;

/**
 * Says what a business booking is: a contract's payment, a private
 * withdrawal or deposit, nothing of these, or whatever the automatic match
 * finds.
 */
export function AssignPaymentDialog({
  transaction,
  match,
  contracts,
  onClose,
}: {
  transaction: BusinessTransaction | null;
  match: PaymentMatch | null;
  contracts: Contract[];
  onClose: () => void;
}) {
  const { t } = useTranslation();
  return (
    <Dialog open={transaction !== null} onOpenChange={(open) => !open && onClose()}>
      {transaction && (
        <DialogContent closeLabel={t("common.close")} className="gap-6 sm:max-w-xl">
          <AssignForm
            key={transaction.id}
            transaction={transaction}
            match={match}
            contracts={contracts}
            onClose={onClose}
          />
        </DialogContent>
      )}
    </Dialog>
  );
}

function AssignForm({
  transaction,
  match,
  contracts,
  onClose,
}: {
  transaction: BusinessTransaction;
  match: PaymentMatch | null;
  contracts: Contract[];
  onClose: () => void;
}) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const [choice, setChoice] = useState<Choice>(() => choiceOf(transaction));
  const save = useMutation({
    mutationFn: () => {
      const [kind, contract] = choice.split(":");
      return api.assignPayment(transaction.id, kind as PaymentKind | "AUTO", contract);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["business-transactions"] });
      onClose();
    },
  });
  const selectable = contracts.filter(
    (contract) => contractStatus(contract, transaction.date) !== "ended" || contract.id === transaction.assignment?.contract,
  );
  const options: { value: Choice; label: string; hint?: string }[] = [
    {
      value: "AUTO",
      label: t("business.assign_auto"),
      hint: match?.automatic ? matchLabel(match, t) : undefined,
    },
    ...selectable.map((contract) => ({
      value: `CONTRACT:${contract.id}`,
      label: contract.customer,
      hint: `${formatMoney(contract.amount)} ${t(`interval.${contract.interval}`)}`,
    })),
    { value: "WITHDRAWAL", label: t("business.kind_WITHDRAWAL") },
    { value: "DEPOSIT", label: t("business.kind_DEPOSIT") },
    { value: "NONE", label: t("business.kind_NONE") },
  ];
  return (
    <>
      <div className="flex items-center gap-4 pr-8">
        <div className="min-w-0 flex-1">
          <DialogTitle className="truncate pr-0">
            {transaction.counterparty || t("transactions.no_counterparty")}
          </DialogTitle>
          <DialogDescription>{formatDate(transaction.date)}</DialogDescription>
        </div>
        <span
          className={cn(
            "figures shrink-0 text-2xl font-extrabold tracking-[-0.02em]",
            transaction.amount > 0 && "text-positive",
          )}
        >
          {formatMoney(transaction.amount, transaction.currency, { signed: true })}
        </span>
      </div>
      <dl className="grid gap-3 rounded-2xl bg-raised p-4 text-sm sm:grid-cols-[8rem_1fr]">
        <dt className="text-muted-foreground">{t("transactions.reference")}</dt>
        <dd className="break-words">{transaction.description || "–"}</dd>
      </dl>
      <fieldset className="grid gap-3">
        <legend className="mb-3 text-sm font-bold text-label">{t("business.assign")}</legend>
        <div className="grid max-h-80 gap-2 overflow-y-auto sm:grid-cols-2">
          {options.map((option) => (
            <button
              key={option.value}
              type="button"
              aria-pressed={choice === option.value}
              onClick={() => setChoice(option.value)}
              className={cn(
                "grid rounded-xl px-3 py-2 text-left text-sm ring-1 ring-divider transition-colors hover:bg-raised",
                choice === option.value && "bg-brand/12 font-bold text-nav-active-text ring-brand/40",
              )}
            >
              <span className="truncate">{option.label}</span>
              {option.hint && <span className="truncate text-xs font-normal text-muted-foreground">{option.hint}</span>}
            </button>
          ))}
        </div>
      </fieldset>
      {save.isError && <Notice>{t("common.error")}</Notice>}
      <div className="flex flex-wrap justify-end gap-2">
        <DialogClose asChild>
          <Button variant="outline">{t("common.cancel")}</Button>
        </DialogClose>
        <Button loading={save.isPending} onClick={() => save.mutate()}>
          {t("common.save")}
        </Button>
      </div>
    </>
  );
}
