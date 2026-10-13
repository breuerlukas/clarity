import { useId, useState, type FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { FolderSearch } from "lucide-react";
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
import { useSpace } from "@/hooks/use-space";
import { api, type Mailbox, type MailboxDraft, type MailSecurity } from "@/lib/api";
import { invoiceError } from "@/lib/invoices";

const DEFAULT_PORT: Record<MailSecurity, number> = { SSL: 993, STARTTLS: 143 };

interface Form {
  host: string;
  port: string;
  security: MailSecurity;
  username: string;
  password: string;
  incoming_folder: string;
  outgoing_folder: string;
}

const formOf = (mailbox: Mailbox | null): Form => ({
  host: mailbox?.host ?? "",
  port: String(mailbox?.port ?? DEFAULT_PORT.SSL),
  security: mailbox?.security ?? "SSL",
  username: mailbox?.username ?? "",
  password: "",
  incoming_folder: mailbox?.incoming_folder ?? "INBOX",
  outgoing_folder: mailbox?.outgoing_folder ?? "",
});

const draftOf = (form: Form): MailboxDraft => ({
  host: form.host.trim(),
  port: Number(form.port),
  security: form.security,
  username: form.username.trim(),
  password: form.password,
  incoming_folder: form.incoming_folder.trim(),
  outgoing_folder: form.outgoing_folder.trim(),
});

/** The usual names mail servers give the folder of sent mail. */
const SENT_FOLDER = /(^|[./])(sent|gesendet|gesendete|sent items|sent messages|gesendete elemente|gesendete objekte)$/i;

/**
 * Connects a mailbox, or changes one when given. The folders can be read
 * from the server once the login is filled in, so the sent folder does not
 * have to be guessed.
 */
export function MailboxDialog({
  open,
  mailbox,
  onOpenChange,
}: {
  open: boolean;
  mailbox: Mailbox | null;
  onOpenChange: (open: boolean) => void;
}) {
  const { t } = useTranslation();
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent closeLabel={t("common.cancel")} className="sm:max-w-xl">
        {open && <MailboxForm key={mailbox?.id ?? "new"} mailbox={mailbox} onDone={() => onOpenChange(false)} />}
      </DialogContent>
    </Dialog>
  );
}

function MailboxForm({ mailbox, onDone }: { mailbox: Mailbox | null; onDone: () => void }) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const space = useSpace();
  const securityId = useId();
  const folderList = useId();
  const [form, setForm] = useState<Form>(() => formOf(mailbox));
  const [confirmDelete, setConfirmDelete] = useState(false);
  const done = async () => {
    await queryClient.invalidateQueries({ queryKey: ["mailboxes"] });
    await queryClient.invalidateQueries({ queryKey: ["invoices"] });
    onDone();
  };
  const folders = useMutation({
    mutationFn: () => api.mailFolders(mailbox?.id ?? null, draftOf(form)),
    onSuccess: ({ folders: names }) => {
      const sent = names.find((name) => SENT_FOLDER.test(name));
      setForm((previous) => ({
        ...previous,
        incoming_folder: names.includes(previous.incoming_folder) ? previous.incoming_folder : (names[0] ?? ""),
        outgoing_folder: previous.outgoing_folder || sent || "",
      }));
    },
  });
  const save = useMutation({
    mutationFn: () =>
      mailbox ? api.updateMailbox(mailbox.id, draftOf(form)) : api.createMailbox(space.id, draftOf(form)),
    onSuccess: done,
  });
  const remove = useMutation({
    mutationFn: () => api.deleteMailbox(mailbox!.id),
    onSuccess: done,
  });
  const set = (key: keyof Form) => (value: string) => setForm((previous) => ({ ...previous, [key]: value }));
  const setSecurity = (security: MailSecurity) =>
    setForm((previous) => ({
      ...previous,
      security,
      port: previous.port === String(DEFAULT_PORT[previous.security]) ? String(DEFAULT_PORT[security]) : previous.port,
    }));
  const canLoadFolders = form.host.trim() && form.username.trim() && (form.password || mailbox);
  const submit = (event: FormEvent) => {
    event.preventDefault();
    save.mutate();
  };
  const error = save.error ?? remove.error ?? folders.error;
  return (
    <>
      <div className="grid gap-2">
        <DialogTitle>{t(mailbox ? "invoices.edit_mailbox" : "invoices.add_mailbox")}</DialogTitle>
        <DialogDescription>{t("invoices.mailbox_description")}</DialogDescription>
      </div>
      <form onSubmit={submit} className="grid gap-4">
        <div className="grid gap-4 sm:grid-cols-[1fr_6rem_9rem]">
          <Field
            label={t("invoices.host")}
            value={form.host}
            onChange={(event) => set("host")(event.target.value)}
            placeholder="imap.example.com"
            maxLength={253}
            autoComplete="off"
            required
          />
          <Field
            label={t("invoices.port")}
            type="number"
            min={1}
            max={65535}
            value={form.port}
            onChange={(event) => set("port")(event.target.value)}
            required
          />
          <div className="grid content-start gap-2">
            <Label htmlFor={securityId}>{t("invoices.security")}</Label>
            <select
              id={securityId}
              value={form.security}
              onChange={(event) => setSecurity(event.target.value as MailSecurity)}
              className="h-12 rounded-2xl border-[1.5px] border-transparent bg-ground px-4 text-sm text-foreground outline-none focus-visible:border-primary"
            >
              <option value="SSL">SSL/TLS</option>
              <option value="STARTTLS">STARTTLS</option>
            </select>
          </div>
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field
            label={t("invoices.username")}
            value={form.username}
            onChange={(event) => set("username")(event.target.value)}
            maxLength={254}
            autoComplete="off"
            required
          />
          <Field
            label={t("invoices.password")}
            hint={mailbox ? t("invoices.password_keep") : undefined}
            type="password"
            value={form.password}
            onChange={(event) => set("password")(event.target.value)}
            maxLength={512}
            autoComplete="new-password"
            required={!mailbox}
          />
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field
            label={t("invoices.incoming_folder")}
            hint={t("invoices.incoming_folder_hint")}
            value={form.incoming_folder}
            onChange={(event) => set("incoming_folder")(event.target.value)}
            list={folderList}
            maxLength={200}
            autoComplete="off"
            required
          />
          <Field
            label={t("invoices.outgoing_folder")}
            hint={t("invoices.outgoing_folder_hint")}
            value={form.outgoing_folder}
            onChange={(event) => set("outgoing_folder")(event.target.value)}
            list={folderList}
            maxLength={200}
            autoComplete="off"
          />
        </div>
        <datalist id={folderList}>
          {folders.data?.folders.map((folder) => (
            <option key={folder} value={folder} />
          ))}
        </datalist>
        <div className="flex flex-wrap items-center gap-3">
          <Button
            type="button"
            variant="outline"
            disabled={!canLoadFolders || folders.isPending}
            onClick={() => folders.mutate()}
          >
            <FolderSearch />
            {t("invoices.load_folders")}
          </Button>
          {folders.data && (
            <span className="text-sm text-muted-foreground" aria-live="polite">
              {t("invoices.folders_found", { count: folders.data.folders.length })}
            </span>
          )}
        </div>
        {error && <Notice>{invoiceError(t, error)}</Notice>}
        <div className="flex flex-wrap items-center justify-end gap-2">
          {mailbox && (
            <Button
              type="button"
              variant={confirmDelete ? "destructive" : "link"}
              className={confirmDelete ? "mr-auto" : "mr-auto px-0 text-muted-foreground"}
              disabled={remove.isPending}
              onClick={() => (confirmDelete ? remove.mutate() : setConfirmDelete(true))}
            >
              {t(confirmDelete ? "invoices.remove_mailbox_confirm" : "invoices.remove_mailbox")}
            </Button>
          )}
          <DialogClose asChild>
            <Button type="button" variant="outline">
              {t("common.cancel")}
            </Button>
          </DialogClose>
          <Button type="submit" disabled={save.isPending}>
            {save.isPending ? t("invoices.connecting") : t(mailbox ? "common.save" : "invoices.connect")}
          </Button>
        </div>
      </form>
    </>
  );
}
