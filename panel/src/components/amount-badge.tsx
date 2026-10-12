import { formatMoney } from "@/lib/format";
import { cn } from "@/lib/utils";

/**
 * An amount as a pill: money in on a soft green, money out on a soft red,
 * both muted enough to sit in long lists. Transfers between own accounts are
 * only outlined, they are neither earned nor spent.
 */
export function AmountBadge({
  value,
  currency = "EUR",
  neutral = false,
  className,
}: {
  value: number;
  currency?: string;
  /** For money moved between own accounts: neither earned nor spent. */
  neutral?: boolean;
  className?: string;
}) {
  return (
    <span
      className={cn(
        "figures inline-flex items-center rounded-full px-2.5 py-0.5 text-[13px] font-bold whitespace-nowrap",
        !neutral && value > 0 && "bg-positive/10 text-positive",
        !neutral && value < 0 && "bg-negative/8 text-negative",
        (neutral || value === 0) && "text-muted-foreground ring-1 ring-divider ring-inset",
        className,
      )}
    >
      {formatMoney(value, currency, { signed: true })}
    </span>
  );
}
