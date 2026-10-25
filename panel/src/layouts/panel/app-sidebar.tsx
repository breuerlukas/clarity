import { useTranslation } from "react-i18next";
import type { TFunction } from "i18next";
import {
  ArrowLeftRight,
  ChartColumnBig,
  FileSignature,
  Landmark,
  LayoutDashboard,
  PiggyBank,
  ReceiptText,
  Repeat,
} from "lucide-react";
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarHeader,
} from "@/components/ui/sidebar";
import { useSpace, useSpacePath } from "@/hooks/use-space";
import type { SpaceKind } from "@/lib/api";
import { SidebarNavigation, type NavGroup } from "@/layouts/panel/navigation";
import { SidebarUser } from "@/layouts/panel/sidebar-user";
import { SpaceSwitcher } from "@/layouts/panel/space-switcher";

/** The pages of a space, which depend on its kind. */
function spaceNavigation(
  t: TFunction,
  kind: SpaceKind,
  path: (page?: string) => string,
): NavGroup[] {
  const pages =
    kind === "BUSINESS"
      ? [
          { title: t("nav.overview"), url: path(), icon: LayoutDashboard },
          { title: t("nav.contracts"), url: path("/contracts"), icon: FileSignature },
          { title: t("nav.invoices"), url: path("/invoices"), icon: ReceiptText },
        ]
      : [
          { title: t("nav.overview"), url: path(), icon: LayoutDashboard },
          { title: t("nav.transactions"), url: path("/transactions"), icon: ArrowLeftRight },
          { title: t("nav.subscriptions"), url: path("/subscriptions"), icon: Repeat },
          { title: t("nav.goals"), url: path("/goals"), icon: PiggyBank },
          { title: t("nav.statistics"), url: path("/statistics"), icon: ChartColumnBig },
        ];
  return [
    { items: pages },
    {
      label: t("nav.banking"),
      items: [{ title: t("nav.accounts"), url: path("/accounts"), icon: Landmark }],
    },
  ];
}

/**
 * The light sidebar: the space switcher behind the mark, the open space's
 * pages, and the owner at the foot.
 */
export function AppSidebar() {
  const { t } = useTranslation();
  const space = useSpace();
  const path = useSpacePath();
  return (
    <Sidebar collapsible="offcanvas" className="z-40">
      <SidebarHeader className="px-3 pt-2.5 pb-2.5">
        <SpaceSwitcher />
      </SidebarHeader>
      <SidebarContent className="px-3">
        <SidebarNavigation groups={spaceNavigation(t, space.kind, path)} />
      </SidebarContent>
      <SidebarFooter className="gap-0 px-3 pt-0 pb-2">
        <SidebarUser />
      </SidebarFooter>
    </Sidebar>
  );
}
