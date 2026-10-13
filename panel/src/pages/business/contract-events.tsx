import { useTranslation } from "react-i18next";
import { CalendarClock } from "lucide-react";
import { Panel } from "@/components/panel";
import type { ContractEvent } from "@/lib/business";
import { formatDate, formatMoney } from "@/lib/format";
import { cn } from "@/lib/utils";

/** Within this many days a deadline or end is due soon and stands out. */
export const SOON_DAYS = 30;

/**
 * Deadlines to cancel, renewals, ends and starts in the next 90 days, the
 * ones within a month highlighted.
 */
export function ContractEvents({ events, className }: { events: ContractEvent[]; className?: string }) {
  const { t } = useTranslation();
  return (
    <Panel title={t("business.events")} aside={t("business.events_note")} className={className}>
      {events.length === 0 && <p className="text-muted-foreground">{t("business.events_empty")}</p>}
      <ul className="zebra -mx-2">
        {events.map((event) => {
          const soon = event.days <= SOON_DAYS && event.kind !== "start";
          return (
            <li
              key={`${event.contract.id}-${event.kind}`}
              className="zebra-row grid grid-cols-[auto_1fr_auto] items-center gap-3 rounded-xl px-2 py-2.5"
            >
              <span
                className={cn(
                  "grid size-9 place-items-center rounded-xl ring-1 ring-divider",
                  soon && "bg-warning/12 text-warning ring-warning/30",
                )}
              >
                <CalendarClock className="size-4" aria-hidden />
              </span>
              <span className="grid min-w-0 gap-0.5">
                <span className="truncate font-medium">{event.contract.customer}</span>
                <span className={cn("text-sm text-muted-foreground", soon && "font-bold text-warning")}>
                  {t(`business.event_${event.kind}`, { date: formatDate(event.date) })}
                </span>
              </span>
              <span className="grid justify-items-end gap-0.5 text-right">
                <span className="figures text-sm font-bold">
                  {formatMoney(event.contract.amount)}
                </span>
                <span className="text-xs text-muted-foreground">
                  {event.days === 0 ? t("business.today") : t("overview.in_days", { days: event.days })}
                </span>
              </span>
            </li>
          );
        })}
      </ul>
    </Panel>
  );
}
