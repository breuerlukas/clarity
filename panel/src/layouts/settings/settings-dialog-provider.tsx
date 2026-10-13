import { useState, type ReactNode } from "react";
import { SettingsDialog, type SettingsSectionId } from "@/layouts/settings/settings-dialog";
import { SettingsDialogContext } from "@/layouts/settings/settings-dialog-context";

/** Holds the one settings dialog, so the sidebar and the owner menu share it. */
export function SettingsDialogProvider({ children }: { children: ReactNode }) {
  const [open, setOpen] = useState(false);
  const [section, setSection] = useState<SettingsSectionId>("appearance");
  const openSettings = (next?: SettingsSectionId) => {
    setSection(next ?? "appearance");
    setOpen(true);
  };
  return (
    <SettingsDialogContext.Provider value={openSettings}>
      {children}
      <SettingsDialog
        open={open}
        onOpenChange={setOpen}
        section={section}
        onSectionChange={setSection}
      />
    </SettingsDialogContext.Provider>
  );
}
