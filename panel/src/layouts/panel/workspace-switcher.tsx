import { useNavigate } from "react-router";
import { useTranslation } from "react-i18next";
import { BriefcaseBusiness, Check, ChevronsUpDown, Wallet, type LucideIcon } from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useSidebar } from "@/components/ui/sidebar";
import { Mark } from "@/components/wordmark";
import { useWorkspace, WORKSPACE_HOME, type Workspace } from "@/layouts/panel/workspace";

const WORKSPACES: { value: Workspace; icon: LucideIcon }[] = [
  { value: "personal", icon: Wallet },
  { value: "business", icon: BriefcaseBusiness },
];

/**
 * The mark and name at the top of the sidebar, with the open workspace
 * beneath. Opens a menu to switch between personal and business, which
 * lands on that workspace's overview.
 */
export function WorkspaceSwitcher() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { isMobile, setOpenMobile } = useSidebar();
  const workspace = useWorkspace();
  const choose = (next: Workspace) => {
    setOpenMobile(false);
    navigate(WORKSPACE_HOME[next]);
  };
  return (
    <DropdownMenu>
      <DropdownMenuTrigger
        aria-label={t("workspace.switch")}
        className="flex w-full items-center gap-2.5 rounded-[10px] px-1.5 py-1.5 text-left transition-colors hover:bg-panel data-[state=open]:bg-panel"
      >
        <Mark className="size-8" />
        <span className="grid min-w-0 flex-1 leading-tight">
          <b className="truncate text-base tracking-tight text-foreground">Clarity</b>
          <span className="truncate text-xs font-bold text-label">
            {t(`workspace.${workspace}`)}
          </span>
        </span>
        <ChevronsUpDown size={16} className="mr-1 shrink-0 text-nav-icon" aria-hidden />
      </DropdownMenuTrigger>
      <DropdownMenuContent
        className="w-(--radix-dropdown-menu-trigger-width) min-w-60 rounded-2xl"
        side={isMobile ? "bottom" : "right"}
        align="start"
        sideOffset={8}
      >
        <DropdownMenuLabel className="text-xs text-muted-foreground">
          {t("workspace.title")}
        </DropdownMenuLabel>
        {WORKSPACES.map((option) => (
          <DropdownMenuItem
            key={option.value}
            onClick={() => choose(option.value)}
            aria-current={option.value === workspace ? "true" : undefined}
            className="gap-2.5 p-2"
          >
            <span className="grid size-8 shrink-0 place-items-center rounded-[9px] ring-1 ring-divider">
              <option.icon className="size-4" aria-hidden />
            </span>
            <span className="grid min-w-0 flex-1">
              <span className="truncate font-bold">{t(`workspace.${option.value}`)}</span>
              <span className="truncate text-xs text-muted-foreground">
                {t(`workspace.${option.value}_hint`)}
              </span>
            </span>
            {option.value === workspace && <Check className="ml-auto size-4 text-brand" aria-hidden />}
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
