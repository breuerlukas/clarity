import { formatMoney } from "@/lib/format";
import { cn } from "@/lib/utils";

/**
 * Money in shows in green with a plus; money out stays in ink. Spending is
 * the normal case, so it does not get an alarm color.
 */
export function Amount({
  value,
  currency = "EUR",
  className,
}: {
  value: number;
  currency?: string;
  className?: string;
}) {
  return (
    <span
      className={cn(
        "figures whitespace-nowrap",
        value > 0 && "text-positive",
        className,
      )}
    >
      {formatMoney(value, currency, { signed: value > 0 })}
    </span>
  );
}