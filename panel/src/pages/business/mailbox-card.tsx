import { useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Mail, Pencil, Plus, RefreshCw } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Notice } from "@/components/notice";
import { Panel } from "@/components/panel";
import { useMailboxes } from "@/hooks/use-business";
import { useSpace } from "@/hooks/use-space";
import { api, type Mailbox } from "@/lib/api";
import { formatRelative } from "@/lib/format";
import { MailboxDialog } from "@/pages/business/mailbox-dialog";

/** The api reads the mails in the background; the list is fetched again after this long. */
const CHECK_REFRESH_MS = 8000;

/**
 * The mailboxes invoices come from, when each was last read and why it
 * failed if it did, and a button to read them now instead of waiting for
 * the next round.
 */
export function MailboxCard() {
  const { t } = useTranslation();
  const space = useSpace();
  const queryClient = useQueryClient();
  const mailboxes = useMailboxes();
  const [editing, setEditing] = useState<Mailbox | null>(null);
  const [creating, setCreating] = useState(false);
  const timer = useRef<ReturnType<typeof setTimeout>>(undefined);
  useEffect(() => () => clearTimeout(timer.current), []);
  const refresh = () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: ["mailboxes"] }),
      queryClient.invalidateQueries({ queryKey: ["invoices"] }),
      queryClient.invalidateQueries({ queryKey: ["business-transactions"] }),
    ]);
  const check = useMutation({
    mutationFn: () => api.checkMailboxes(space.id),
    onSuccess: async () => {
      await refresh();
      clearTimeout(timer.current);
      timer.current = setTimeout(refresh, CHECK_REFRESH_MS);
    },
  });
  return (
    <Panel title={t("invoices.mailboxes")}>
      <p className="text-sm text-muted-foreground">{t("invoices.mailboxes_hint")}</p>
      {mailboxes.isError && <Notice className="text-sm">{t("common.error")}</Notice>}
      {mailboxes.data?.length === 0 && <p className="text-muted-foreground">{t("invoices.mailboxes_empty")}</p>}
      <ul className="grid gap-2">
        {mailboxes.data?.map((mailbox) => (
          <li key={mailbox.id} className="flex items-start gap-3 rounded-2xl px-3 py-3 ring-1 ring-divider">
            <span className="grid size-9 shrink-0 place-items-center rounded-xl bg-raised">
              <Mail className="size-4" aria-hidden />
            </span>
            <span className="grid min-w-0 flex-1 gap-0.5">
              <span className="truncate font-medium">{mailbox.username}</span>
              <span className="truncate text-sm text-muted-foreground">
                {mailbox.host} · {[mailbox.incoming_folder, mailbox.outgoing_folder].filter(Boolean).join(", ")}
              </span>
              {mailbox.last_error ? (
                <Notice className="text-sm">
                  {t("invoices.mailbox_failed", {
                    error: t(`invoices.mailbox_errors.${mailbox.last_error}`, {
                      defaultValue: t("invoices.mailbox_errors.CONNECTION"),
                    }),
                  })}
                </Notice>
              ) : (
                <span className="text-sm text-muted-foreground">
                  {mailbox.last_checked_at
                    ? t("invoices.last_checked", { time: formatRelative(mailbox.last_checked_at) })
                    : t("invoices.never_checked")}
                </span>
              )}
            </span>
            <Button size="icon-sm" variant="ghost" onClick={() => setEditing(mailbox)} aria-label={t("invoices.edit_mailbox")}>
              <Pencil />
            </Button>
          </li>
        ))}
      </ul>
      <div className="flex flex-wrap gap-2">
        {(mailboxes.data?.length ?? 0) > 0 && (
          <Button variant="outline" disabled={check.isPending} onClick={() => check.mutate()}>
            <RefreshCw className={check.isPending ? "animate-spin" : ""} />
            {t("invoices.check_now")}
          </Button>
        )}
        <Button variant={mailboxes.data?.length ? "ghost" : "default"} onClick={() => setCreating(true)}>
          <Plus />
          {t("invoices.add_mailbox")}
        </Button>
      </div>
      {check.data?.started === false && (
        <p className="text-sm text-muted-foreground" aria-live="polite">{t("invoices.check_busy")}</p>
      )}
      {check.data?.started && (
        <p className="text-sm text-muted-foreground" aria-live="polite">{t("invoices.check_started")}</p>
      )}
      {check.isError && <Notice className="text-sm">{t("common.error")}</Notice>}
      <MailboxDialog
        open={creating || editing !== null}
        mailbox={editing}
        onOpenChange={(open) => {
          if (!open) {
            setCreating(false);
            setEditing(null);
          }
        }}
      />
    </Panel>
  );
}
