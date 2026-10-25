import { useTranslation } from "react-i18next";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { Segmented } from "@/components/segmented";
import { isoDate } from "@/lib/dates";
import {
  currentPeriod,
  customPeriod,
  isCurrentPeriod,
  periodLabel,
  shiftPeriod,
  type Period,
  type PeriodKind,
} from "@/lib/period";

/**
 * Pick how far to look (month, quarter, year, last twelve months, days of
 * one's own or everything) and step through it with the arrows. One click
 * gets from "October" to "all of 2026".
 */
export function PeriodPicker({
  period,
  kinds,
  onChange,
}: {
  period: Period;
  kinds: PeriodKind[];
  onChange: (period: Period) => void;
}) {
  const { t } = useTranslation();
  const stepable = !["rolling", "custom", "all"].includes(period.kind);
  return (
    <div className="flex flex-wrap items-center gap-2">
      <Segmented<PeriodKind>
        label={t("period.label")}
        value={period.kind}
        options={kinds.map((kind) => ({ value: kind, label: t(`period.${kind}`) }))}
        onChange={(kind) => onChange(currentPeriod(kind))}
      />
      {period.kind === "custom" && <CustomDays period={period} onChange={onChange} />}
      {stepable && (
        <div className="flex items-center rounded-full bg-ground p-1">
          <StepButton label={t("period.previous")} onClick={() => onChange(shiftPeriod(period, -1))}>
            <ChevronLeft className="size-4" />
          </StepButton>
          <span className="min-w-28 px-1 text-center text-sm font-bold" aria-live="polite">
            {periodLabel(period)}
          </span>
          <StepButton
            label={t("period.next")}
            disabled={isCurrentPeriod(period)}
            onClick={() => onChange(shiftPeriod(period, 1))}
          >
            <ChevronRight className="size-4" />
          </StepButton>
        </div>
      )}
    </div>
  );
}

function StepButton({
  label,
  disabled,
  onClick,
  children,
}: {
  label: string;
  disabled?: boolean;
  onClick: () => void;
  children: React.ReactNode;
}) {
  return (
    <button
      type="button"
      aria-label={label}
      disabled={disabled}
      onClick={onClick}
      className="grid size-8 place-items-center rounded-full text-muted-foreground hover:bg-card hover:text-foreground disabled:opacity-40"
    >
      {children}
    </button>
  );
}

/** From and to of a custom period; neither reaches past today. */
function CustomDays({ period, onChange }: { period: Period; onChange: (period: Period) => void }) {
  const { t } = useTranslation();
  const [from, to] = period.key.split("_");
  const today = isoDate(new Date());
  const field =
    "h-10 rounded-full border-[1.5px] border-transparent bg-ground px-3 text-sm text-foreground outline-none focus-visible:border-primary";
  return (
    <div className="flex items-center gap-1.5">
      <input
        type="date"
        aria-label={t("period.from")}
        value={from}
        max={today}
        onChange={(event) => event.target.value && onChange(customPeriod(event.target.value, to))}
        className={field}
      />
      <span className="text-muted-foreground" aria-hidden>
        –
      </span>
      <input
        type="date"
        aria-label={t("period.to")}
        value={to}
        max={today}
        onChange={(event) => event.target.value && onChange(customPeriod(from, event.target.value))}
        className={field}
      />
    </div>
  );
}
