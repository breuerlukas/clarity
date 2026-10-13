import { useTranslation } from "react-i18next";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { Segmented } from "@/components/segmented";
import {
  currentPeriod,
  isCurrentPeriod,
  periodLabel,
  shiftPeriod,
  type Period,
  type PeriodKind,
} from "@/lib/period";

/**
 * Pick how far to look (month, quarter, year, last twelve months) and step
 * through it with the arrows. One click gets from "October" to "all of 2026".
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
  const stepable = period.kind !== "rolling";
  return (
    <div className="flex flex-wrap items-center gap-2">
      <Segmented<PeriodKind>
        label={t("period.label")}
        value={period.kind}
        options={kinds.map((kind) => ({ value: kind, label: t(`period.${kind}`) }))}
        onChange={(kind) => onChange(currentPeriod(kind))}
      />
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
