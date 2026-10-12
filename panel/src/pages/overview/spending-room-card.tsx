import { Trans, useTranslation } from "react-i18next";
import { AmountBadge } from "@/components/amount-badge";
import type { SpendingRoom } from "@/lib/budget";
import { formatDate, formatMoney } from "@/lib/format";
import { cn } from "@/lib/utils";

/**
 * What is really free until the next salary, after the fixed costs that are
 * still coming, and how that number comes about.
 */
export function SpendingRoomCard({ room }: { room: SpendingRoom }) {
  const { t } = useTranslation();
  const short = room.room < 0;
  const day = Math.max(1, room.cycleDays - room.daysLeft + 1);
  return (
    <section className="grid gap-6 rounded-3xl border bg-card p-6 sm:p-7">
      <div className="flex flex-wrap items-start justify-between gap-x-6 gap-y-4">
        <div className="grid gap-3">
          <h2 className="text-sm font-bold text-label">
            {room.basedOnSalary
              ? t("room.title_salary", { date: formatDate(room.payday) })
              : t("room.title_month_end")}
          </h2>
          <p
            className={cn(
              "figures text-[clamp(2.5rem,6vw,3.75rem)] leading-none font-extrabold tracking-[-0.03em]",
              short && "text-warning",
            )}
          >
            {formatMoney(room.room)}
          </p>
          <p className="text-[15px] text-muted-foreground">
            {short ? (
              t("room.short")
            ) : (
              <Trans
                i18nKey="room.per_day"
                count={room.daysLeft}
                values={{ amount: formatMoney(room.perDay), days: room.daysLeft }}
                components={{ strong: <strong className="figures font-extrabold text-foreground" /> }}
              />
            )}
          </p>
        </div>
        <div className="grid min-w-56 gap-2 text-sm">
          <Row label={t("room.everyday")}>
            <span className="figures font-bold">{formatMoney(room.balance)}</span>
          </Row>
          <Row label={t("room.fixed_until_payday", { count: room.upcoming.length })}>
            <AmountBadge value={-room.reserved} />
          </Row>
        </div>
      </div>
      <div className="grid gap-1.5">
        <div className="h-2 overflow-hidden rounded-full bg-raised">
          <div className="h-full rounded-full bg-primary" style={{ width: `${room.progress * 100}%` }} />
        </div>
        <span className="text-xs text-muted-foreground">
          {t("room.progress", { day, days: room.cycleDays })}
        </span>
      </div>
    </section>
  );
}

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex items-center justify-between gap-6">
      <span className="text-muted-foreground">{label}</span>
      {children}
    </div>
  );
}
