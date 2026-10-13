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
}: {
  label: string;
  value: T | undefined;
  options: { value: T; label: string; icon?: LucideIcon}[];
  onChange: (value: T) => void;
}) {
  const { container, pill } = useSlidingPill<HTMLDivElement>('[aria-checked="true"]');
  return (
    <div ref={container} role="radiogroup" aria-label={label} className="relative flex w-fit max-w-full flex-wrap gap-1 rounded-full bg-ground p-1">
      <span ref={pill} aria-hidden="true" className={cn(PILL_CLASS, "bg-card shadow-sm ring-1 ring-divider")} />
      {options.map((option) => (
        <button
          key={option.value}
          type="button"
          role="radio"
          aria-checked={value === option.value}
          onClick={() => onChange(option.value)}
          className={cn(
            "relative inline-flex h-9 items-center gap-2 rounded-full px-4 text-sm font-bold transition-colors duration-300",
            value === option.value ? "text-foreground" : "text-muted-foreground hover:text-foreground",
          )}
        >
          {option.icon && <option.icon className="size-4" aria-hidden />}
          {option.label}
        </button>
      ))}
    </div>
  );
}
