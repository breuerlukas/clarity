import { useId } from "react";
import { useCategories, useCategoryLook } from "@/hooks/use-categories";
import { Label } from "@/components/ui/label";
import { pickableCategories } from "@/lib/categories";
import type { Category, CategoryKind } from "@/lib/api";
import { cn } from "@/lib/utils";

/** A category picked from the native select, the fitting kind first. */
export function CategoryField({
  label,
  value,
  kind,
  onChange,
  className,
}: {
  /** Visible above the select; without one it is only read out. */
  label: string;
  value: Category;
  kind?: CategoryKind;
  onChange: (category: Category) => void;
  className?: string;
}) {
  const id = useId();
  const categories = useCategories();
  const look = useCategoryLook();
  return (
    <div className={cn("grid content-start gap-2", className)}>
      <Label htmlFor={id}>{label}</Label>
      <select
        id={id}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="h-12 rounded-2xl border-[1.5px] border-transparent bg-ground px-4 text-sm text-foreground outline-none focus-visible:border-primary"
      >
        {pickableCategories(categories.data ?? [], value, kind).map((category) => (
          <option key={category.key} value={category.key}>
            {look(category.key).label}
          </option>
        ))}
      </select>
    </div>
  );
}
