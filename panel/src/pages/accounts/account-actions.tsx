import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { ArrowRightLeft, Eye, EyeOff } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogTitle } from "@/components/ui/dialog";
import { Notice } from "@/components/notice";
import { useSpace, useSpaceName, useSpaces } from "@/hooks/use-space";
import { api, type Account } from "@/lib/api";
import { spaceIcon } from "@/lib/space-icons";

/**
 * Moves one account to another space. Its transactions, fixed costs and
 * statistics follow it; the bank connection stays where it was.
 */
export function MoveAccount({ account }: { account: Account }) {
  const { t } = useTranslation();
  const space = useSpace();
  const spaces = useSpaces();
  const nameOf = useSpaceName();
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const move = useMutation({
    mutationFn: (target: string) => api.moveAccount(account.id, target),
    onSuccess: async () => {
      await queryClient.invalidateQueries();
      setOpen(false);
    },
  });
  const targets = spaces.data?.filter((entry) => entry.id !== space.id) ?? [];
  if (targets.length === 0) {
    return null;
  }
  return (
    <>
      <Button variant="ghost" size="sm" className="text-muted-foreground" onClick={() => setOpen(true)}>
        <ArrowRightLeft />
        {t("accounts.move")}
      </Button>
      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent closeLabel={t("common.cancel")}>
          <div className="grid gap-2">
            <DialogTitle>{t("accounts.move_title", { account: account.name })}</DialogTitle>
            <DialogDescription>{t("accounts.move_hint")}</DialogDescription>
          </div>
          <ul className="grid gap-1">
            {targets.map((target) => {
              const Icon = spaceIcon(target.icon);
              return (
                <li key={target.id}>
                  <button
                    type="button"
                    disabled={move.isPending}
                    onClick={() => move.mutate(target.id)}
                    className="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left transition-colors hover:bg-raised disabled:opacity-50"
                  >
                    <Icon className="size-4 text-muted-foreground" aria-hidden />
                    <span className="font-medium">{nameOf(target)}</span>
                  </button>
                </li>
              );
            })}
          </ul>
          {move.isError && <Notice>{t("common.error")}</Notice>}
        </DialogContent>
      </Dialog>
    </>
  );
}

/**
 * Hides an account from every view but this page for a while, or shows it
 * again. It keeps syncing meanwhile.
 */
export function HideAccount({ account }: { account: Account }) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const toggle = useMutation({
    mutationFn: () => api.hideAccount(account.id, !account.hidden),
    onSuccess: () => queryClient.invalidateQueries(),
  });
  return (
    <Button variant="ghost" size="sm" className="text-muted-foreground" disabled={toggle.isPending} onClick={() => toggle.mutate()}>
      {account.hidden ? <Eye /> : <EyeOff />}
      {t(account.hidden ? "accounts.show" : "accounts.hide")}
    </Button>
  );
}
