import { Outlet } from "react-router";
import { SidebarInset, SidebarProvider } from "@/components/ui/sidebar";
import { AppSidebar } from "@/layouts/panel/app-sidebar";
import { AppHeader } from "@/layouts/panel/header";
import { SettingsDialogProvider } from "@/layouts/settings/settings-dialog-provider";

/**
 * Sidebar, topbar and the page. Content grows with the screen up to 1760 px
 * (the 1920 px reference minus gutters), then centers.
 */
export function PanelLayout() {
  return (
    <SidebarProvider>
      <SettingsDialogProvider>
        <AppSidebar />
        <SidebarInset className="min-w-0">
          <AppHeader />
          <div className="mx-auto flex w-full max-w-[1840px] min-w-0 flex-1 flex-col px-4 py-7 sm:px-10 sm:pt-9 sm:pb-10">
            <Outlet />
          </div>
        </SidebarInset>
      </SettingsDialogProvider>
    </SidebarProvider>
  );
}
