import type { ReactNode } from "react";
import type { LucideIcon } from "lucide-react";

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
  options: { value: T; label: string; icon?: LucideIcon; leading?: ReactNode }[];
  onChange: (value: T) => void;
}) {
  return (
    <div role="radiogroup" aria-label={label} className="flex flex-wrap gap-1 rounded-full bg-ground p-1">
      {options.map((option) => (
        <button
          key={option.value}
          type="button"
          role="radio"
          aria-checked={value === option.value}
          onClick={() => onChange(option.value)}
          className={
            value === option.value
              ? "inline-flex h-9 items-center gap-2 rounded-full bg-card px-4 text-sm font-bold text-foreground shadow-sm ring-1 ring-divider"
              : "inline-flex h-9 items-center gap-2 rounded-full px-4 text-sm font-bold text-muted-foreground ring-1 ring-transparent hover:text-foreground"
          }
        >
          {option.leading}
          {option.icon && <option.icon className="size-4" aria-hidden />}
          {option.label}
        </button>
      ))}
    </div>
  );
}
