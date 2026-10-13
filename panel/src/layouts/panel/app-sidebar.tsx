import { useTranslation } from "react-i18next";
import type { TFunction } from "i18next";
import {
  ArrowLeftRight,
  ChartColumnBig,
  FileSignature,
  Landmark,
  LayoutDashboard,
  Repeat,
} from "lucide-react";
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarHeader,
} from "@/components/ui/sidebar";
import { SidebarNavigation, type NavGroup } from "@/layouts/panel/navigation";
import { SidebarUser } from "@/layouts/panel/sidebar-user";
import { useWorkspace } from "@/layouts/panel/workspace";
import { WorkspaceSwitcher } from "@/layouts/panel/workspace-switcher";

export function appNavigation(t: TFunction): NavGroup[] {
  return [
    {
      items: [
        { title: t("nav.overview"), url: "/", icon: LayoutDashboard },
        { title: t("nav.transactions"), url: "/transactions", icon: ArrowLeftRight },
        { title: t("nav.subscriptions"), url: "/subscriptions", icon: Repeat },
        { title: t("nav.statistics"), url: "/statistics", icon: ChartColumnBig },
      ],
    },
    {
      label: t("nav.banking"),
      items: [{ title: t("nav.accounts"), url: "/accounts", icon: Landmark }],
    },
  ];
}

export function businessNavigation(t: TFunction): NavGroup[] {
  return [
    {
      items: [
        { title: t("nav.business_overview"), url: "/business", icon: LayoutDashboard },
        { title: t("nav.contracts"), url: "/business/contracts", icon: FileSignature },
      ],
    },
    {
      label: t("nav.banking"),
      items: [{ title: t("nav.accounts"), url: "/business/accounts", icon: Landmark }],
    },
  ];
}

/**
 * The light sidebar: the workspace switcher behind the mark, the open
 * workspace's navigation, and the owner at the foot.
 */
export function AppSidebar() {
  const { t } = useTranslation();
  const workspace = useWorkspace();
  return (
    <Sidebar collapsible="offcanvas" className="z-40">
      <SidebarHeader className="px-3 pt-2.5 pb-2.5">
        <WorkspaceSwitcher />
      </SidebarHeader>
      <SidebarContent className="px-3">
        <SidebarNavigation
          groups={workspace === "business" ? businessNavigation(t) : appNavigation(t)}
        />
      </SidebarContent>
      <SidebarFooter className="gap-0 px-3 pt-0 pb-2">
        <SidebarUser />
      </SidebarFooter>
    </Sidebar>
  );
}
