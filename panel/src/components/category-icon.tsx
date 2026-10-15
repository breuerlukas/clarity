import { createElement } from "react";
import { useCategoryLook } from "@/hooks/use-categories";
import type { Category } from "@/lib/api";
import { categoryColorStyle, categoryIcon } from "@/lib/category-icons";
import { cn } from "@/lib/utils";

/** The category label always sits next to it; the icon only helps scanning. */
export function CategoryIcon({
  category,
  className,
}: {
  category: Category;
  className?: string;
}) {
  const look = useCategoryLook()(category);
  return <CategoryBadge icon={look.icon} color={look.color} className={className} />;
}

/** An icon in its color, also for a category that isn't saved yet. */
export function CategoryBadge({
  icon,
  color,
  className,
}: {
  icon: string;
  color: string;
  className?: string;
}) {
  return (
    <span
      className={cn(
        "grid size-9 shrink-0 place-items-center rounded-full bg-card text-muted-foreground ring-1 ring-line",
        className,
      )}
      style={categoryColorStyle(color)}
      aria-hidden="true"
    >
      {createElement(categoryIcon(icon), { className: "size-4" })}
    </span>
  );
}