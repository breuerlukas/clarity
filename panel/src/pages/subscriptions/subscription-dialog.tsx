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
import { AmountBadge } from "@/components/amount-badge";
import { Field } from "@/components/field";
import { Notice } from "@/components/notice";
import { useHistory } from "@/hooks/use-finances";
import { useSpace } from "@/hooks/use-space";
import { api, ApiError, type RecurringPayment, type RecurringSettingDraft } from "@/lib/api";
import { formatDate } from "@/lib/format";
import { recurringBookings } from "@/lib/overview";

/**
 * What the owner knows better than the bookings: that a fixed cost was
 * cancelled and when it ends, that a missing one still runs, that it is no
 * fixed cost at all, and the notice
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
    ignored: payment.ignored,
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
        <Bookings payment={payment} />
        {save.isError && (
          <Notice>
            {save.error instanceof ApiError && save.error.code === 3008
              ? t("subscriptions.errors.3008")
              : t("common.error")}
          </Notice>
        )}
        <div className="flex flex-wrap items-center justify-end gap-2">
          <Button
            type="button"
            variant="link"
            className="mr-auto px-0 text-muted-foreground"
            disabled={save.isPending}
            onClick={() => save.mutate({ ignored: !payment.ignored })}
          >
            {t(payment.ignored ? "subscriptions.count_again" : "subscriptions.ignore")}
          </Button>
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

/** The bookings it was found in, from the last 13 months the panel holds. */
function Bookings({ payment }: { payment: RecurringPayment }) {
  const { t } = useTranslation();
  const history = useHistory();
  const bookings = recurringBookings(payment, history.data ?? []);
  return (
    <section className="grid gap-2">
      <h3 className="text-sm font-bold text-label">{t("subscriptions.bookings", { count: bookings.length })}</h3>
      {history.data && bookings.length === 0 && <p className="text-sm text-muted-foreground">{t("subscriptions.no_bookings")}</p>}
      <ul className="zebra -mx-2 max-h-56 overflow-y-auto">
        {bookings.map((booking) => (
          <li key={booking.id} className="zebra-row flex items-center justify-between gap-3 rounded-xl px-2 py-1.5 text-sm">
            <span className="min-w-0">
              <span className="block">{formatDate(booking.date)}</span>
              {booking.description && <span className="block truncate text-xs text-muted-foreground">{booking.description}</span>}
            </span>
            <AmountBadge value={booking.amount} currency={booking.currency} />
          </li>
        ))}
      </ul>
    </section>
  );
}
