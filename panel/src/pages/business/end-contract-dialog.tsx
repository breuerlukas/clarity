import { useState, type FormEvent } from "react";
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
import { Field } from "@/components/field";
import { Notice } from "@/components/notice";
import { api, ApiError, type Contract } from "@/lib/api";
import { nextExit } from "@/lib/business";
import { isoDate } from "@/lib/dates";
import { formatDate } from "@/lib/format";

/**
 * Ends a contract on a chosen last day, suggesting the earliest one the
 * notice period allows. An earlier day is possible, for an agreed end.
 */
export function EndContractDialog({
  contract,
  onClose,
}: {
  contract: Contract | null;
  onClose: () => void;
}) {
  const { t } = useTranslation();
  return (
    <Dialog open={contract !== null} onOpenChange={(open) => !open && onClose()}>
      {contract && (
        <DialogContent closeLabel={t("common.cancel")}>
          <EndContractForm key={contract.id} contract={contract} onClose={onClose} />
        </DialogContent>
      )}
    </Dialog>
  );
}

function EndContractForm({ contract, onClose }: { contract: Contract; onClose: () => void }) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const [exit] = useState(() => nextExit(contract, isoDate(new Date())));
  const [endDate, setEndDate] = useState(exit.end);
  const end = useMutation({
    mutationFn: () => api.endContract(contract.id, endDate),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["contracts"] });
      onClose();
    },
  });
  const submit = (event: FormEvent) => {
    event.preventDefault();
    end.mutate();
  };
  return (
    <>
      <div className="grid gap-2">
        <DialogTitle>{t("business.end_title", { customer: contract.customer })}</DialogTitle>
        <DialogDescription>
          {t("business.end_hint", { end: formatDate(exit.end), deadline: formatDate(exit.deadline) })}
        </DialogDescription>
      </div>
      <form onSubmit={submit} className="grid gap-4">
        <Field
          label={t("business.end_date")}
          type="date"
          min={contract.start_date}
          value={endDate}
          onChange={(event) => setEndDate(event.target.value)}
          required
        />
        {end.isError && (
          <Notice>
            {end.error instanceof ApiError && end.error.code === 4002
              ? t("business.errors.4002")
              : t("common.error")}
          </Notice>
        )}
        <div className="flex flex-wrap justify-end gap-2">
          <DialogClose asChild>
            <Button type="button" variant="outline">
              {t("common.cancel")}
            </Button>
          </DialogClose>
          <Button type="submit" variant="destructive" disabled={end.isPending || !endDate}>
            {t("business.end")}
          </Button>
        </div>
      </form>
    </>
  );
}
