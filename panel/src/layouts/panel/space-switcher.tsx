import { useState } from "react";
import { useNavigate } from "react-router";
import { useTranslation } from "react-i18next";
import { Check, ChevronsUpDown, Plus, Settings } from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useSidebar } from "@/components/ui/sidebar";
import { SpaceIcon } from "@/components/space-icon";
import { Mark } from "@/components/wordmark";
import { spacePath, useSpace, useSpaceName, useSpaces } from "@/hooks/use-space";
import type { Space } from "@/lib/api";
import { SpaceDialog } from "@/layouts/panel/space-dialog";

/**
 * The mark and name at the top of the sidebar, with the open space beneath.
 * Opens a menu with every space, each with its own settings, and a way to
 * add another; picking one lands on its overview.
 */
export function SpaceSwitcher() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { isMobile, setOpenMobile } = useSidebar();
  const spaces = useSpaces();
  const current = useSpace();
  const nameOf = useSpaceName();
  const [dialog, setDialog] = useState<{ space: Space | null } | null>(null);
  const open = (space: Space) => {
    setOpenMobile(false);
    navigate(spacePath(space));
  };
  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger
          aria-label={t("spaces.switch")}
          className="flex w-full items-center gap-2.5 rounded-[10px] px-1.5 py-1.5 text-left transition-colors hover:bg-panel data-[state=open]:bg-panel"
        >
          <Mark className="size-8" />
          <span className="grid min-w-0 flex-1 leading-tight">
            <b className="truncate text-base tracking-tight text-foreground">Clarity</b>
            <span className="flex min-w-0 items-center gap-1 text-xs font-bold text-label">
              <SpaceIcon icon={current.icon} className="size-3 shrink-0" />
              <span className="truncate">{nameOf(current)}</span>
            </span>
          </span>
          <ChevronsUpDown size={16} className="mr-1 shrink-0 text-nav-icon" aria-hidden />
        </DropdownMenuTrigger>
        <DropdownMenuContent
          className="w-(--radix-dropdown-menu-trigger-width) min-w-64 rounded-2xl"
          side={isMobile ? "bottom" : "right"}
          align="start"
          sideOffset={8}
        >
          <DropdownMenuLabel className="text-xs text-muted-foreground">{t("spaces.title")}</DropdownMenuLabel>
          {spaces.data?.map((space) => (
            <div key={space.id} className="flex items-center gap-0.5">
              <DropdownMenuItem
                onClick={() => open(space)}
                aria-current={space.id === current.id ? "true" : undefined}
                className="min-w-0 flex-1 gap-2.5 p-2"
              >
                <span className="grid size-8 shrink-0 place-items-center rounded-[9px] ring-1 ring-divider">
                  <SpaceIcon icon={space.icon} className="size-4" />
                </span>
                <span className="grid min-w-0 flex-1">
                  <span className="truncate font-bold">{nameOf(space)}</span>
                  <span className="truncate text-xs text-muted-foreground">{t(`spaces.kind_${space.kind}`)}</span>
                </span>
                {space.id === current.id && <Check className="ml-auto size-4 text-brand" aria-hidden />}
              </DropdownMenuItem>
              <DropdownMenuItem
                onClick={() => setDialog({ space })}
                aria-label={t("spaces.settings", { name: nameOf(space) })}
                className="size-9 shrink-0 justify-center p-0 text-muted-foreground"
              >
                <Settings className="size-4" aria-hidden />
              </DropdownMenuItem>
            </div>
          ))}
          <DropdownMenuSeparator />
          <DropdownMenuItem className="gap-2.5 p-2" onClick={() => setDialog({ space: null })}>
            <span className="grid size-8 shrink-0 place-items-center rounded-[9px] border border-dashed border-divider">
              <Plus className="size-4" aria-hidden />
            </span>
            <span className="font-bold text-muted-foreground">{t("spaces.create")}</span>
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
      <SpaceDialog
        open={dialog !== null}
        space={dialog?.space ?? null}
        onOpenChange={(next) => !next && setDialog(null)}
      />
    </>
  );
}
