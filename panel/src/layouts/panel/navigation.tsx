import { Link, useLocation } from "react-router";
import type { LucideIcon } from "lucide-react";
import { useSidebar } from "@/components/ui/sidebar";
import { cn } from "@/lib/utils";

export interface NavItem {
  title: string;
  url: string;
  icon: LucideIcon;
}

export interface NavGroup {
  label?: string;
  items: NavItem[];
}

/**
 * The entry a path belongs to: the longest url it equals or sits under, so
 * "/settings/security" still lights "Settings".
 */
export function activeUrl(pathname: string, urls: string[]): string | undefined {
  const path = pathname.replace(/\/+$/, "") || "/";
  return urls
    .filter((url) => path === url || (url !== "/" && path.startsWith(url + "/")))
    .sort((left, right) => right.length - left.length)[0];
}

export function SidebarNavigation({ groups }: { groups: NavGroup[] }) {
  const { pathname } = useLocation();
  const active = activeUrl(
    pathname,
    groups.flatMap((group) => group.items.map((item) => item.url)),
  );
  return (
    <nav className="flex flex-col">
      {groups.map((group, index) => (
        <div key={group.label ?? index} className="flex flex-col gap-px">
          {group.label && (
            <span className="mx-2.5 mt-3.5 mb-1 text-xs font-bold text-label">
              {group.label}
            </span>
          )}
          {group.items.map((item) => (
            <SidebarLink key={item.url} item={item} active={item.url === active} />
          ))}
        </div>
      ))}
    </nav>
  );
}

/** Active: a soft green tint, green icon and a short bar at the left edge. */
function SidebarLink({ item, active }: { item: NavItem; active: boolean }) {
  const { isMobile, setOpenMobile } = useSidebar();
  const Icon = item.icon;
  return (
    <Link
      to={item.url}
      aria-current={active ? "page" : undefined}
      onClick={() => isMobile && setOpenMobile(false)}
      className={cn(
        "relative flex h-[38px] items-center gap-[11px] rounded-[10px] px-2.5 text-sm transition-colors",
        active
          ? "bg-brand/12 font-extrabold text-nav-active-text before:absolute before:inset-y-1.5 before:-left-3 before:w-1 before:rounded-r-full before:bg-brand"
          : "text-nav-text hover:bg-panel hover:text-foreground",
      )}
    >
      <Icon size={18} className={active ? "text-brand" : "text-nav-icon"} aria-hidden />
      <span className="truncate">{item.title}</span>
    </Link>
  );
}
