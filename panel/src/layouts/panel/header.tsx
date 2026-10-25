import { Fragment } from "react";
import { Link, useLocation } from "react-router";
import { useTranslation } from "react-i18next";
import type { TFunction } from "i18next";
import { PanelLeft } from "lucide-react";
import { useSidebar } from "@/components/ui/sidebar";
import { spacePath, useSpace, useSpaceName } from "@/hooks/use-space";
import { HideAmountsButton } from "@/layouts/panel/hide-amounts-button";
import type { Space } from "@/lib/api";
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

/** The trail for a path: the space, then the page in it. */
function crumbsFor(pathname: string, space: Space, spaceName: string, t: TFunction): Crumb[] {
  const [, , page] = pathname.split("/").filter(Boolean);
  return [
    { title: spaceName, href: spacePath(space) },
    { title: t(page ? `nav.${page}` : "nav.overview") },
  ];
}

/** The 64 px topbar: the sidebar toggle, where you are and the amounts eye. */
export function AppHeader() {
  const { t } = useTranslation();
  const { pathname } = useLocation();
  const { toggleSidebar } = useSidebar();
  const space = useSpace();
  const nameOf = useSpaceName();
  const crumbs = crumbsFor(pathname, space, nameOf(space), t);
  return (
    <header className="flex h-16 shrink-0 items-center gap-2 border-b border-divider px-3 sm:px-4">
      <button
        type="button"
        onClick={toggleSidebar}
        aria-label={t("nav.toggle_sidebar")}
        className="grid size-9 shrink-0 place-items-center rounded-[10px] text-muted-foreground transition-colors hover:bg-panel hover:text-foreground"
      >
        <PanelLeft size={18} aria-hidden />
      </button>
      <span className="mr-2 h-5 w-px bg-divider" aria-hidden="true" />
      <Breadcrumb className="min-w-0">
        <BreadcrumbList className="flex-nowrap">
          {crumbs.map((crumb, index) => (
            <Fragment key={crumb.title}>
              {index > 0 && <BreadcrumbSeparator className="hidden sm:inline-flex [&>svg]:size-3.5" />}
              <BreadcrumbItem className={index < crumbs.length - 1 ? "hidden sm:inline-flex" : "min-w-0"}>
                {crumb.href ? (
                  <BreadcrumbLink asChild>
                    <Link to={crumb.href} className="text-muted-foreground hover:text-foreground">
                      {crumb.title}
                    </Link>
                  </BreadcrumbLink>
                ) : (
                  <BreadcrumbPage className="truncate font-semibold">{crumb.title}</BreadcrumbPage>
                )}
              </BreadcrumbItem>
            </Fragment>
          ))}
        </BreadcrumbList>
      </Breadcrumb>
      <span className="ml-auto flex items-center gap-3">
        {import.meta.env.VITE_DEMO === "true" && <DemoBadge />}
        <HideAmountsButton />
      </span>
    </header>
  );
}

/** Tells visitors of the public demo that nothing here is real. */
function DemoBadge() {
  const { t } = useTranslation();
  const source = import.meta.env.VITE_REPOSITORY_URL;
  return (
    <span className="flex items-center gap-3 text-sm">
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
