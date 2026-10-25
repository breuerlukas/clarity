import { useState } from "react";
import { Trans, useTranslation } from "react-i18next";
import { Pencil } from "lucide-react";
import { AmountBadge } from "@/components/amount-badge";
import { Button } from "@/components/ui/button";
import type { MonthlyBudget, SpendingRoom } from "@/lib/budget";
import { formatDate, formatMoney } from "@/lib/format";
import { cn } from "@/lib/utils";
import { MonthlyBudgetDialog } from "@/pages/overview/monthly-budget-dialog";

/**
 * How much of the money meant for this pay cycle is left, and per day. The
 * accounts only give context: what lies there, minus fixed costs still due
 * and money set aside, and a warning when that is less than the budget.
 * Without a budget and without a known salary it asks for one.
 */
export function SpendingRoomCard({ room, budget }: { room: SpendingRoom; budget: MonthlyBudget | null }) {
  const { t } = useTranslation();
  const [editing, setEditing] = useState(false);
  const day = Math.max(1, room.cycleDays - room.daysLeft + 1);
  const over = budget !== null && budget.left < 0;
  const uncovered = budget !== null && budget.left > room.room;
  return (
    <section className="grid gap-6 rounded-3xl border bg-card p-6 sm:p-7">
      <div className="flex flex-wrap items-start justify-between gap-x-6 gap-y-4">
        <div className="grid gap-3">
          <h2 className="flex items-center gap-1 text-sm font-bold text-label">
            {room.basedOnSalary ? t("room.title_salary", { date: formatDate(room.payday) }) : t("room.title_month")}
            <Button variant="ghost" size="icon" className="size-7" onClick={() => setEditing(true)} aria-label={t("room.edit_budget")}>
              <Pencil className="size-3.5" />
            </Button>
          </h2>
          {budget ? (
            <>
              <p className="flex flex-wrap items-baseline gap-x-3">
                <span className={cn("figures text-[clamp(2.5rem,6vw,3.75rem)] leading-none font-extrabold tracking-[-0.03em]", over && "text-warning")}>
                  {formatMoney(budget.left)}
                </span>
                <span className="figures text-muted-foreground">
                  {t(budget.suggested ? "room.of_suggested" : "room.of_budget", { limit: formatMoney(budget.limit) })}
                </span>
              </p>
              <p className="text-[15px] text-muted-foreground">
                {over ? (
                  t("room.over", { amount: formatMoney(-budget.left) })
                ) : (
                  <Trans
                    i18nKey="room.per_day"
                    count={room.daysLeft}
                    values={{ amount: formatMoney(budget.perDay), days: room.daysLeft }}
                    components={{ strong: <strong className="figures font-extrabold text-foreground" /> }}
                  />
                )}
              </p>
            </>
          ) : (
            <Button className="justify-self-start" onClick={() => setEditing(true)}>
              {t("room.set_budget")}
            </Button>
          )}
        </div>
        <div className="grid min-w-56 gap-2 text-sm">
          <Row label={t("room.everyday")}>
            <span className="figures font-bold">{formatMoney(room.balance)}</span>
          </Row>
          <Row label={t("room.fixed_until_payday", { count: room.upcoming.length })}>
            <AmountBadge value={-room.reserved} />
          </Row>
          {room.setAside > 0 && (
            <Row label={t("room.set_aside")}>
              <AmountBadge value={-room.setAside} />
            </Row>
          )}
          <Row label={t("room.on_accounts")}>
            <span className={cn("figures font-bold", room.room < 0 && "text-warning")}>{formatMoney(room.room)}</span>
          </Row>
        </div>
      </div>
      {uncovered && <p className="text-sm font-bold text-warning">{t("room.uncovered", { amount: formatMoney(room.room) })}</p>}
      <div className="grid gap-1.5">
        <div className="h-2 overflow-hidden rounded-full bg-raised">
          <div
            className={cn("h-full rounded-full bg-primary", over && "bg-warning")}
            style={{ width: `${(budget ? budget.share : room.progress) * 100}%` }}
          />
        </div>
        <span className="text-xs text-muted-foreground">
          {budget
            ? t("room.spent", { amount: formatMoney(budget.spent), day, days: room.cycleDays })
            : t("room.progress", { day, days: room.cycleDays })}
        </span>
      </div>
      <MonthlyBudgetDialog open={editing} budget={budget} onClose={() => setEditing(false)} />
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
