import { ChevronLeft, ChevronRight } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Button } from "@/components/ui/button";
import { currentMonth, shiftMonth, type MonthKey } from "@/lib/dates";
import { formatMonth } from "@/lib/format";

export function MonthSwitcher({
  month,
  onChange,
}: {
  month: MonthKey;
  onChange: (month: MonthKey) => void;
}) {
  const { t } = useTranslation();
  const isCurrent = month >= currentMonth();
  return (
    <div className="flex items-center gap-1">
      <Button
        variant="ghost"
        size="icon"
        onClick={() => onChange(shiftMonth(month, -1))}
        aria-label={t("common.previous_month")}
      >
        <ChevronLeft className="size-5" />
      </Button>
      <h2
        className="min-w-[9.5rem] text-center text-[17px] font-semibold"
        aria-live="polite"
      >
        {formatMonth(month)}
      </h2>
      <Button
        variant="ghost"
        size="icon"
        disabled={isCurrent}
        onClick={() => onChange(shiftMonth(month, 1))}
        aria-label={t("common.next_month")}
      >
        <ChevronRight className="size-5" />
      </Button>
    </div>
  );
}