import { useTranslation } from "react-i18next";
import { Settings2, Shapes, ShieldCheck, UserRound, type LucideIcon } from "lucide-react";
import { Dialog, DialogContent, DialogDescription, DialogTitle } from "@/components/ui/dialog";
import { cn } from "@/lib/utils";
import { AppearanceSection } from "@/pages/settings/appearance-section";
import { AccountSection } from "@/pages/settings/account-section";
import { CategoriesSection } from "@/pages/settings/categories-section";
import { SecuritySection } from "@/pages/settings/security-section";
import packageInformation from "@/../package.json";

export type SettingsSectionId = "appearance" | "categories" | "account" | "security";

const SECTIONS: { id: SettingsSectionId; icon: LucideIcon; Content: () => React.ReactNode }[] = [
  { id: "appearance", icon: Settings2, Content: AppearanceSection },
  { id: "categories", icon: Shapes, Content: CategoriesSection },
  { id: "account", icon: UserRound, Content: AccountSection },
  { id: "security", icon: ShieldCheck, Content: SecuritySection },
];

/**
 * The settings as a dialog over the page: sections on the left, the chosen
 * one on the right, the version at the foot. On phones the sections become
 * a row of tabs above the content. The content scrolls in a positioned box:
 * Radix puts the hidden input of a switch inside a form absolutely, and
 * without it that input stretched the whole dialog below the form.
 */
export function SettingsDialog({
  open,
  onOpenChange,
  section,
  onSectionChange,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  section: SettingsSectionId;
  onSectionChange: (section: SettingsSectionId) => void;
}) {
  const { t } = useTranslation();
  const current = SECTIONS.find((entry) => entry.id === section) ?? SECTIONS[0];
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent
        closeLabel={t("common.close")}
        className="gap-0 p-0 sm:max-w-[880px] sm:p-0"
      >
        <DialogTitle className="sr-only">{t("nav.settings")}</DialogTitle>
        <DialogDescription className="sr-only">{t(`settings.description_${current.id}`)}</DialogDescription>
        <div className="flex min-h-0 min-w-0 flex-col sm:h-[580px] sm:flex-row">
          <nav
            aria-label={t("nav.settings")}
            className="flex shrink-0 gap-1 overflow-x-auto border-b border-divider bg-raised p-3 pr-14 sm:w-[220px] sm:pr-4 sm:flex-col sm:overflow-visible sm:rounded-l-3xl sm:border-r sm:border-b-0 sm:p-4"
          >
            <span className="hidden px-3 pt-1 pb-3 text-sm font-extrabold sm:block">
              {t("nav.settings")}
            </span>
            {SECTIONS.map(({ id, icon: Icon }) => {
              const active = id === current.id;
              return (
                <button
                  key={id}
                  type="button"
                  aria-current={active ? "page" : undefined}
                  onClick={() => onSectionChange(id)}
                  className={cn(
                    "flex h-10 shrink-0 items-center gap-2.5 rounded-xl px-3 text-sm whitespace-nowrap transition-colors",
                    active
                      ? "bg-brand/12 font-extrabold text-nav-active-text"
                      : "text-nav-text hover:bg-panel hover:text-foreground",
                  )}
                >
                  <Icon size={17} className={active ? "text-brand" : "text-nav-icon"} aria-hidden />
                  {t(`settings.section_${id}`)}
                </button>
              );
            })}
            <span className="mt-auto hidden px-3 text-xs text-muted-foreground sm:block">
              {t("settings.version", { version: packageInformation.version })}
            </span>
          </nav>
          <div className="relative min-w-0 flex-1 overflow-y-auto p-5 sm:p-7">
            <current.Content />
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
