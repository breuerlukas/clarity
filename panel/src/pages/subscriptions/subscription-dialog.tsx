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
import { useSpace } from "@/hooks/use-space";
import { api, ApiError, type RecurringPayment, type RecurringSettingDraft } from "@/lib/api";
import { formatDate } from "@/lib/format";

/**
 * What the owner knows better than the bookings: that a fixed cost was
 * cancelled and when it ends, that a missing one still runs, and the notice
 * period to be reminded of before it renews.
 */
export function SubscriptionDialog({
  payment,
  onClose,
}: {
  payment: RecurringPayment | null;
  onClose: () => void;
}) {
  const { t } = useTranslation();
  return (
    <Dialog open={payment !== null} onOpenChange={(open) => !open && onClose()}>
      {payment && (
        <DialogContent closeLabel={t("common.cancel")}>
          <SubscriptionForm key={payment.key} payment={payment} onClose={onClose} />
        </DialogContent>
      )}
    </Dialog>
  );
}

function SubscriptionForm({ payment, onClose }: { payment: RecurringPayment; onClose: () => void }) {
  const { t } = useTranslation();
  const space = useSpace();
  const queryClient = useQueryClient();
  const [cancelledOn, setCancelledOn] = useState(payment.cancelled_on ?? "");
  const [noticeDays, setNoticeDays] = useState(payment.notice_days?.toString() ?? "");
  const draft: RecurringSettingDraft = {
    cancelled_on: cancelledOn,
    dismissed_last_date: payment.dismissed_last_date ?? "",
    notice_days: noticeDays,
  };
  const save = useMutation({
    mutationFn: (changes: Partial<RecurringSettingDraft>) =>
      api.updateRecurringSetting(space.id, payment.key, { ...draft, ...changes }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["recurring"] });
      onClose();
    },
  });
  const submit = (event: FormEvent) => {
    event.preventDefault();
    save.mutate({});
  };
  const stopped = payment.status !== "ACTIVE" && !payment.cancelled_on;
  return (
    <>
      <div className="grid gap-2">
        <DialogTitle>{payment.counterparty}</DialogTitle>
        <DialogDescription>
          {stopped
            ? t(`subscriptions.status_hint_${payment.status}`, { date: formatDate(payment.last_date) })
            : t("subscriptions.edit_hint")}
        </DialogDescription>
      </div>
      <form onSubmit={submit} className="grid gap-4">
        {stopped && (
          <Button
            type="button"
            variant="outline"
            disabled={save.isPending}
            onClick={() => save.mutate({ dismissed_last_date: payment.last_date })}
          >
            {t("subscriptions.not_ended")}
          </Button>
        )}
        <Field
          label={t("subscriptions.cancelled_on")}
          type="date"
          value={cancelledOn}
          onChange={(event) => setCancelledOn(event.target.value)}
          hint={t("subscriptions.cancelled_on_hint")}
        />
        <Field
          label={t("subscriptions.notice_days")}
          type="number"
          inputMode="numeric"
          min="0"
          max="366"
          value={noticeDays}
          onChange={(event) => setNoticeDays(event.target.value)}
          placeholder={t("subscriptions.notice_days_placeholder")}
          hint={t("subscriptions.notice_days_hint")}
        />
        {save.isError && (
          <Notice>
            {save.error instanceof ApiError && save.error.code === 3008
              ? t("subscriptions.errors.3008")
              : t("common.error")}
          </Notice>
        )}
        <div className="flex flex-wrap justify-end gap-2">
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
