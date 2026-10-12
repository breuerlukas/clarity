import { Fragment } from "react";
import { Link, useLocation } from "react-router";
import { useTranslation } from "react-i18next";
import type { TFunction } from "i18next";
import { PanelLeft } from "lucide-react";
import { useSidebar } from "@/components/ui/sidebar";
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from "@/components/ui/breadcrumb";

interface Crumb {
  title: string;
  href?: string;
}

/** The trail for a path; every page sits directly under the root today. */
function crumbsFor(pathname: string, t: TFunction): Crumb[] {
  const [section] = pathname.split("/").filter(Boolean);
  if (!section) {
    return [{ title: t("nav.overview") }];
  }
  return [{ title: t(`nav.${section}`) }];
}

/** The 64 px topbar: the sidebar toggle and where you are. */
export function AppHeader() {
  const { t } = useTranslation();
  const { pathname } = useLocation();
  const { toggleSidebar } = useSidebar();
  const crumbs = crumbsFor(pathname, t);
  return (
    <header className="flex h-16 shrink-0 items-center gap-3.5 border-b border-divider px-4 sm:px-7">
      <button
        type="button"
        onClick={toggleSidebar}
        aria-label={t("nav.toggle_sidebar")}
        className="grid size-9 shrink-0 place-items-center rounded-[10px] text-muted-foreground transition-colors hover:bg-panel hover:text-foreground"
      >
        <PanelLeft size={18} aria-hidden />
      </button>
      <Breadcrumb className="min-w-0">
        <BreadcrumbList className="flex-nowrap">
          {crumbs.map((crumb, index) => (
            <Fragment key={crumb.title}>
              {index > 0 && <BreadcrumbSeparator className="hidden sm:inline-flex" />}
              <BreadcrumbItem className={index < crumbs.length - 1 ? "hidden sm:inline-flex" : "min-w-0"}>
                {crumb.href ? (
                  <BreadcrumbLink asChild>
                    <Link to={crumb.href}>{crumb.title}</Link>
                  </BreadcrumbLink>
                ) : (
                  <BreadcrumbPage className="truncate text-[15px]">{crumb.title}</BreadcrumbPage>
                )}
              </BreadcrumbItem>
            </Fragment>
          ))}
        </BreadcrumbList>
      </Breadcrumb>
      {import.meta.env.VITE_DEMO === "true" && <DemoBadge />}
    </header>
  );
}

/** Tells visitors of the public demo that nothing here is real. */
function DemoBadge() {
  const { t } = useTranslation();
  const source = import.meta.env.VITE_REPOSITORY_URL;
  return (
    <span className="ml-auto flex items-center gap-3 text-sm">
      <span className="rounded-full bg-brand/12 px-3 py-1 font-bold text-brand-text">
        {t("common.demo")}
      </span>
      {source && (
        <a href={source} className="hidden font-bold text-muted-foreground hover:text-foreground sm:inline">
          {t("common.demo_source")}
        </a>
      )}
    </span>
  );
}
