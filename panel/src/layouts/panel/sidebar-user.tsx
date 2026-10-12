import { useTranslation } from "react-i18next";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  ChevronsUpDown,
  Languages,
  LogOut,
  Monitor,
  Moon,
  Settings,
  Sun,
  SunMoon,
  type LucideIcon,
} from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuSeparator,
  DropdownMenuSub,
  DropdownMenuSubContent,
  DropdownMenuSubTrigger,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useSidebar } from "@/components/ui/sidebar";
import { useSession } from "@/hooks/use-session";
import { api } from "@/lib/api";
import { Flag } from "@/components/flag";
import { LANGUAGES } from "@/locales/i18n";
import { useTheme, type ThemePreference } from "@/lib/theme";
import { useOpenSettings } from "@/layouts/settings/settings-dialog-context";

const THEMES: { value: ThemePreference; icon: LucideIcon }[] = [
  { value: "system", icon: Monitor },
  { value: "light", icon: Sun },
  { value: "dark", icon: Moon },
];

/**
 * The sidebar's last row: the owner's initial and name open a menu with the
 * settings, theme, language and logout.
 */
export function SidebarUser() {
  const { t, i18n } = useTranslation();
  const { isMobile, setOpenMobile } = useSidebar();
  const openSettings = useOpenSettings();
  const session = useSession();
  const logout = useLogout();
  const [theme, setTheme] = useTheme();
  const name = session.data?.name ?? "";
  return (
    <div className="mt-2 flex items-center gap-1 border-t border-divider px-1.5 pt-2">
      <DropdownMenu>
        <DropdownMenuTrigger className="flex min-w-0 flex-1 items-center gap-2.5 rounded-[10px] py-1 text-left">
          <span className="grid size-[30px] shrink-0 place-items-center rounded-full bg-divider text-[13px] font-extrabold text-foreground">
            {name.slice(0, 1).toUpperCase()}
          </span>
          <b className="flex-1 truncate text-sm text-foreground">{name}</b>
          <ChevronsUpDown size={16} className="mr-1 shrink-0 text-nav-icon" aria-hidden />
        </DropdownMenuTrigger>
        <DropdownMenuContent
          className="min-w-56 rounded-2xl"
          side={isMobile ? "top" : "right"}
          align="end"
          sideOffset={8}
        >
          <DropdownMenuLabel className="truncate">{name}</DropdownMenuLabel>
          <DropdownMenuSeparator />
          <DropdownMenuItem
            onClick={() => {
              setOpenMobile(false);
              openSettings();
            }}
          >
            <Settings />
            {t("nav.settings")}
          </DropdownMenuItem>
          <DropdownMenuSub>
            <DropdownMenuSubTrigger>
              <SunMoon />
              {t("settings.theme")}
            </DropdownMenuSubTrigger>
            <DropdownMenuSubContent>
              <DropdownMenuRadioGroup
                value={theme}
                onValueChange={(value) => setTheme(value as ThemePreference)}
              >
                {THEMES.map((option) => (
                  <DropdownMenuRadioItem key={option.value} value={option.value}>
                    <option.icon aria-hidden />
                    {t(`settings.theme_${option.value}`)}
                  </DropdownMenuRadioItem>
                ))}
              </DropdownMenuRadioGroup>
            </DropdownMenuSubContent>
          </DropdownMenuSub>
          <DropdownMenuSub>
            <DropdownMenuSubTrigger>
              <Languages />
              {t("settings.language")}
            </DropdownMenuSubTrigger>
            <DropdownMenuSubContent>
              <DropdownMenuRadioGroup
                value={i18n.resolvedLanguage}
                onValueChange={(value) => i18n.changeLanguage(value)}
              >
                {LANGUAGES.map((language) => (
                  <DropdownMenuRadioItem key={language.code} value={language.code}>
                    <Flag country={language.flag} />
                    {language.name}
                  </DropdownMenuRadioItem>
                ))}
              </DropdownMenuRadioGroup>
            </DropdownMenuSubContent>
          </DropdownMenuSub>
          <DropdownMenuSeparator />
          <DropdownMenuItem onClick={() => logout.mutate()}>
            <LogOut />
            {t("nav.logout")}
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </div>
  );
}

/**
 * Forgets the session (which sends the route guard to the login) and every
 * cached finance query. clear() would detach the guard's own session
 * observer, which then never noticed the logout.
 */
export function useLogout() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: api.logout,
    onSettled: () => {
      queryClient.setQueryData(["session"], null);
      queryClient.removeQueries({
        predicate: (query) => !["session", "setup"].includes(String(query.queryKey[0])),
      });
    },
  });
}

