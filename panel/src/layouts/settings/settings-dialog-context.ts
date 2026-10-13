import { createContext, useContext } from "react";
import type { SettingsSectionId } from "@/layouts/settings/settings-dialog";

export const SettingsDialogContext = createContext<(section?: SettingsSectionId) => void>(
  () => undefined,
);

export function useOpenSettings() {
  return useContext(SettingsDialogContext);
}
