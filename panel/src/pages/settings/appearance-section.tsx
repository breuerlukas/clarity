import { useTranslation } from "react-i18next";
import { Monitor, Moon, Settings2, Sun } from "lucide-react";
import { Flag } from "@/components/flag";
import { LANGUAGES } from "@/locales/i18n";
import { useTheme, type ThemePreference } from "@/lib/theme";
import { Segmented } from "@/components/segmented";
import { SettingRow, SettingsSection } from "@/pages/settings/settings-card";

export function AppearanceSection() {
  const { t, i18n } = useTranslation();
  const [theme, setTheme] = useTheme();
  return (
    <SettingsSection
      icon={Settings2}
      title={t("settings.section_appearance")}
      description={t("settings.description_appearance")}
    >
      <SettingRow label={t("settings.language")} hint={t("settings.language_hint")}>
        <Segmented
          label={t("settings.language")}
          value={i18n.resolvedLanguage}
          options={LANGUAGES.map((language) => ({
            value: language.code as string,
            label: language.name,
            leading: <Flag country={language.flag} />,
          }))}
          onChange={(language) => i18n.changeLanguage(language)}
        />
      </SettingRow>
      <SettingRow label={t("settings.theme")} hint={t("settings.theme_hint")}>
        <Segmented<ThemePreference>
          label={t("settings.theme")}
          value={theme}
          options={[
            { value: "system", label: t("settings.theme_system"), icon: Monitor },
            { value: "light", label: t("settings.theme_light"), icon: Sun },
            { value: "dark", label: t("settings.theme_dark"), icon: Moon },
          ]}
          onChange={setTheme}
        />
      </SettingRow>
    </SettingsSection>
  );
}
