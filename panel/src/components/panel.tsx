import type { ReactNode } from "react";
import { Link } from "react-router";
import { useTranslation } from "react-i18next";
import { ChevronRight } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";

/** A card: white (graphite in dark mode), rounded, with an optional heading. */
export function Panel({
  title,
  aside,
  viewAll,
  children,
  className,
}: {
  title?: string;
  aside?: ReactNode;
  /** Where "View all ›" leads; omit for no link. */
  viewAll?: string;
  children: ReactNode;
  className?: string;
}) {
  return (
    <section
      className={cn(
        "flex min-w-0 flex-col gap-5 rounded-3xl border bg-card p-6",
        className,
      )}
    >
      {title && <CardHeading title={title} aside={aside} viewAll={viewAll} />}
      {children}
    </section>
  );
}

/** A card still loading: a bar for the title, a block for the content. */
export function PanelSkeleton({ className }: { className?: string }) {
  return (
    <Panel className={className}>
      <div className="flex flex-1 flex-col gap-5" aria-busy="true">
        <Skeleton className="h-6 w-36" />
        <Skeleton className="flex-1 rounded-2xl" />
      </div>
    </Panel>
  );
}

/** Every card's header: the title (18/800) left, a link or note right. */
export function CardHeading({
  title,
  aside,
  viewAll,
}: {
  title: ReactNode;
  aside?: ReactNode;
  viewAll?: string;
}) {
  const { t } = useTranslation();
  return (
    <div className="flex min-h-8 flex-wrap items-center justify-between gap-3">
      <h2 className="text-lg leading-tight font-extrabold">{title}</h2>
      {aside && <div className="text-sm text-muted-foreground">{aside}</div>}
      {viewAll && (
        <Link
          to={viewAll}
          className="inline-flex items-center gap-1 rounded-md text-[13px] font-bold text-brand-text hover:underline"
        >
          {t("common.view_all")}
          <ChevronRight className="size-3.5" aria-hidden />
        </Link>
      )}
    </div>
  );
}

/**
 * A page's own heading row: the title (30/800) with an optional muted line
 * beneath, and the page's actions at the right.
 */
export function PageHeader({
  title,
  subtitle,
  children,
}: {
  title: string;
  subtitle?: ReactNode;
  children?: ReactNode;
}) {
  return (
    <div className="mb-[22px] flex flex-wrap items-center gap-x-4 gap-y-3">
      <div className="min-w-[min(100%,12rem)] flex-1">
        <h1 className="text-[30px] leading-tight font-extrabold tracking-[-0.02em] break-words">
          {title}
        </h1>
        {subtitle && <span className="mt-1 block text-sm text-muted-foreground">{subtitle}</span>}
      </div>
      {children && <div className="flex flex-wrap items-center gap-2">{children}</div>}
    </div>
  );
}