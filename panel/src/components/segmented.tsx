import type { LucideIcon } from "lucide-react";
import { PILL_CLASS, useSlidingPill } from "@/hooks/use-sliding-pill";
import { cn } from "@/lib/utils";

/**
 * A row of mutually exclusive choices as pill buttons, the way the theme and
 * language are picked.
 */
export function Segmented<T extends string>({
  label,
  value,
  options,
  onChange,
  size = "default",
}: {
  label: string;
  value: T | undefined;
  options: { value: T; label: string; icon?: LucideIcon}[];
  onChange: (value: T) => void;
  /** Small for a choice inside a list row. */
  size?: "default" | "sm";
}) {
  const { container, pill } = useSlidingPill<HTMLDivElement>('[aria-checked="true"]');
  return (
    <div ref={container} role="radiogroup" aria-label={label} className={cn("relative flex w-fit max-w-full flex-wrap gap-1 rounded-full bg-ground", size === "sm" ? "p-0.5" : "p-1")}>
      <span ref={pill} aria-hidden="true" className={cn(PILL_CLASS, "bg-card shadow-sm ring-1 ring-divider")} />
      {options.map((option) => (
        <button
          key={option.value}
          type="button"
          role="radio"
          aria-checked={value === option.value}
          onClick={() => onChange(option.value)}
          className={cn(
            "relative inline-flex items-center rounded-full font-bold transition-colors duration-300",
            size === "sm" ? "h-7 gap-1.5 px-3 text-xs" : "h-9 gap-2 px-4 text-sm",
            value === option.value ? "text-foreground" : "text-muted-foreground hover:text-foreground",
          )}
        >
          {option.icon && <option.icon className={size === "sm" ? "size-3.5" : "size-4"} aria-hidden />}
          {option.label}
        </button>
      ))}
    </div>
  );
}
