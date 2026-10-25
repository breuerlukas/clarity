import { useId, useState, type FormEvent, type ReactNode } from "react";
import { Trans, useTranslation } from "react-i18next";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Notice } from "@/components/notice";
import { ApiError } from "@/lib/api";

/**
 * The delete link of a form and the popup behind it: nothing goes before
 * the owner typed the name of what goes, or the delete word when it has
 * none. A slip of the mouse can't delete anything.
 */
export function ConfirmDelete({
  label,
  name,
  consequence,
  pending,
  error,
  errors,
  onConfirm,
  trigger,
}: {
  /** The link, e.g. "Delete space". */
  label: string;
  /** What has to be typed; empty asks for the delete word. */
  name: string;
  /** What else goes with it, in one sentence. */
  consequence: string;
  pending: boolean;
  /** What the delete failed with, shown in the popup. */
  error: unknown;
  /** The locale section whose "errors" explain the api's codes. */
  errors: string;
  onConfirm: () => void;
  /** Another way to open it than the link. */
  trigger?: (open: () => void) => ReactNode;
}) {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const code = error instanceof ApiError ? error.code : undefined;
  const message = error ? t(`${errors}.errors.${code}`, { defaultValue: t("common.error") }) : undefined;
  return (
    <>
      {trigger ? (
        trigger(() => setOpen(true))
      ) : (
        <Button type="button" variant="link" className="mr-auto px-0 text-muted-foreground" onClick={() => setOpen(true)}>
          {label}
        </Button>
      )}
      <Dialog open={open} onOpenChange={setOpen}>
        {open && (
          <DialogContent closeLabel={t("common.cancel")}>
            <ConfirmForm
              label={label}
              expected={name.trim() || t("delete.word")}
              consequence={consequence}
              pending={pending}
              error={message}
              onConfirm={onConfirm}
              onCancel={() => setOpen(false)}
            />
          </DialogContent>
        )}
      </Dialog>
    </>
  );
}

function ConfirmForm({
  label,
  expected,
  consequence,
  pending,
  error,
  onConfirm,
  onCancel,
}: {
  label: string;
  expected: string;
  consequence: string;
  pending: boolean;
  error?: string;
  onConfirm: () => void;
  onCancel: () => void;
}) {
  const { t } = useTranslation();
  const inputId = useId();
  const [typed, setTyped] = useState("");
  const matches = typed.trim() === expected;
  const submit = (event: FormEvent) => {
    event.preventDefault();
    if (matches) {
      onConfirm();
    }
  };
  return (
    <form onSubmit={submit} className="grid gap-4">
      <div className="grid gap-2">
        <DialogTitle>{label}</DialogTitle>
        <DialogDescription>
          {consequence} {t("delete.irreversible")}
        </DialogDescription>
      </div>
      <div className="grid gap-2">
        <Label htmlFor={inputId}>
          <Trans i18nKey="delete.type" values={{ name: expected }} components={{ strong: <strong className="font-extrabold" /> }} />
        </Label>
        <Input id={inputId} value={typed} onChange={(event) => setTyped(event.target.value)} autoComplete="off" autoFocus />
      </div>
      {error && <Notice>{error}</Notice>}
      <div className="flex flex-wrap justify-end gap-2">
        <Button type="button" variant="outline" onClick={onCancel}>
          {t("common.cancel")}
        </Button>
        <Button type="submit" variant="destructive" loading={pending} disabled={!matches}>
          {t("delete.confirm")}
        </Button>
      </div>
    </form>
  );
}
