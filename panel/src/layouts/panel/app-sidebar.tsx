import { Link } from "react-router";
import { useTranslation } from "react-i18next";
import type { TFunction } from "i18next";
import {
  ArrowLeftRight,
  ChartColumnBig,
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
import { Wordmark } from "@/components/wordmark";
import { SidebarNavigation, type NavGroup } from "@/layouts/panel/navigation";
import { SidebarUser } from "@/layouts/panel/sidebar-user";

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

/** The light sidebar: mark, navigation, and the owner at the foot. */
export function AppSidebar() {
  const { t } = useTranslation();
  return (
    <Sidebar collapsible="offcanvas" className="z-40">
      <SidebarHeader className="px-3 pt-4 pb-0">
        <Link to="/" className="rounded-[10px] px-1.5 pt-0.5 pb-4">
          <Wordmark />
        </Link>
      </SidebarHeader>
      <SidebarContent className="px-3">
        <SidebarNavigation groups={appNavigation(t)} />
      </SidebarContent>
      <SidebarFooter className="gap-0 px-3 pt-0 pb-2">
        <SidebarUser />
      </SidebarFooter>
    </Sidebar>
  );
}
