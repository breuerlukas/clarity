import { useState, type FormEvent } from "react";
import { useLocation, useNavigate } from "react-router";
import { useTranslation } from "react-i18next";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { BriefcaseBusiness, Wallet } from "lucide-react";
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
import { Segmented } from "@/components/segmented";
import { spacePath, useSpace, useSpaceName } from "@/hooks/use-space";
import { api, ApiError, type Space, type SpaceKind } from "@/lib/api";
import { SPACE_ICONS } from "@/lib/space-icons";
import { cn } from "@/lib/utils";

const DEFAULT_ICON: Record<SpaceKind, string> = {
  PERSONAL: "wallet",
  BUSINESS: "briefcase",
};

/**
 * Creates a space (personal or business, chosen once) or changes the name
 * and icon of one, which can also be deleted here once it is empty.
 */
export function SpaceDialog({
  open,
  space,
  onOpenChange,
}: {
  open: boolean;
  space: Space | null;
  onOpenChange: (open: boolean) => void;
}) {
  const { t } = useTranslation();
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent closeLabel={t("common.cancel")} className="sm:max-w-lg">
        {open && <SpaceForm key={space?.id ?? "new"} space={space} onDone={() => onOpenChange(false)} />}
      </DialogContent>
    </Dialog>
  );
}

/**
 * Renaming the open space keeps the page and follows its new address;
 * deleting it goes to another space.
 */
function SpaceForm({ space, onDone }: { space: Space | null; onDone: () => void }) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const queryClient = useQueryClient();
  const current = useSpace();
  const nameOf = useSpaceName();
  const [kind, setKind] = useState<SpaceKind>(space?.kind ?? "PERSONAL");
  const [name, setName] = useState(space ? nameOf(space) : "");
  const [icon, setIcon] = useState(space?.icon ?? DEFAULT_ICON.PERSONAL);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const isCurrent = space?.id === current.id;
  const save = useMutation({
    mutationFn: () => (space ? api.updateSpace(space.id, name.trim(), icon) : api.createSpace(name.trim(), icon, kind)),
    onSuccess: async ({ space: saved }) => {
      await queryClient.invalidateQueries({ queryKey: ["spaces"] });
      onDone();
      if (!space) {
        navigate(spacePath(saved));
      } else if (isCurrent) {
        navigate(spacePath(saved, pathname.replace(/^\/s\/[^/]+/, "")), { replace: true });
      }
    },
  });
  const remove = useMutation({
    mutationFn: () => api.deleteSpace(space!.id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["spaces"] });
      onDone();
      if (isCurrent) {
        navigate("/", { replace: true });
      }
    },
  });
  const chooseKind = (next: SpaceKind) => {
    if (icon === DEFAULT_ICON[kind]) {
      setIcon(DEFAULT_ICON[next]);
    }
    setKind(next);
  };
  const submit = (event: FormEvent) => {
    event.preventDefault();
    save.mutate();
  };
  const error = save.error ?? remove.error;
  const errorCode = error instanceof ApiError ? error.code : undefined;
  return (
    <>
      <div className="grid gap-2">
        <DialogTitle>{space ? t("spaces.edit", { name: nameOf(space) }) : t("spaces.create")}</DialogTitle>
        <DialogDescription>{t(space ? "spaces.edit_hint" : "spaces.create_hint")}</DialogDescription>
      </div>
      <form onSubmit={submit} className="grid gap-5">
        {!space && (
          <div className="grid gap-2">
            <span className="text-sm font-bold text-label">{t("spaces.kind")}</span>
            <Segmented<SpaceKind>
              label={t("spaces.kind")}
              value={kind}
              options={[
                { value: "PERSONAL", label: t("spaces.kind_PERSONAL"), icon: Wallet },
                { value: "BUSINESS", label: t("spaces.kind_BUSINESS"), icon: BriefcaseBusiness },
              ]}
              onChange={chooseKind}
            />
            <p className="text-sm text-muted-foreground">{t(`spaces.kind_${kind}_hint`)}</p>
          </div>
        )}
        <Field
          label={t("spaces.name")}
          value={name}
          onChange={(event) => setName(event.target.value)}
          maxLength={40}
          placeholder={t(kind === "BUSINESS" ? "spaces.name_placeholder_business" : "spaces.name_placeholder")}
          required
          autoComplete="off"
        />
        <fieldset className="grid gap-2">
          <legend className="mb-2 text-sm font-bold text-label">{t("spaces.icon")}</legend>
          <div className="grid grid-cols-8 gap-1.5">
            {SPACE_ICONS.map((option) => (
              <button
                key={option.id}
                type="button"
                aria-label={option.id}
                aria-pressed={icon === option.id}
                onClick={() => setIcon(option.id)}
                className={cn(
                  "grid aspect-square place-items-center rounded-xl ring-1 ring-divider transition-colors hover:bg-raised",
                  icon === option.id && "bg-brand/12 text-nav-active-text ring-brand/40",
                )}
              >
                <option.icon className="size-4" aria-hidden />
              </button>
            ))}
          </div>
        </fieldset>
        {error && (
          <Notice>
            {errorCode && ["5000", "5002", "5003"].includes(String(errorCode))
              ? t(`spaces.errors.${errorCode}`)
              : t("common.error")}
          </Notice>
        )}
        <div className="flex flex-wrap items-center justify-end gap-2">
          {space && (
            <Button
              type="button"
              variant={confirmDelete ? "destructive" : "link"}
              className={confirmDelete ? "mr-auto" : "mr-auto px-0 text-muted-foreground"}
              disabled={remove.isPending}
              onClick={() => (confirmDelete ? remove.mutate() : setConfirmDelete(true))}
            >
              {t(confirmDelete ? "spaces.delete_confirm" : "spaces.delete")}
            </Button>
          )}
          <DialogClose asChild>
            <Button type="button" variant="outline">
              {t("common.cancel")}
            </Button>
          </DialogClose>
          <Button type="submit" disabled={save.isPending || !name.trim()}>
            {t(space ? "common.save" : "spaces.create_submit")}
          </Button>
        </div>
      </form>
    </>
  );
}
