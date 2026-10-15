import type { Category, CategoryDetails, CategoryKind } from "@/lib/api";

export interface CategoryLook {
  label: string;
  icon: string;
  color: string;
}

/**
 * How a category shows. Until the list has loaded, and for a key deleted in
 * the meantime, the translated label of the key stands in.
 */
export function categoryLook(
  categories: CategoryDetails[] | undefined,
  key: Category,
  translatedLabel: (key: Category) => string,
): CategoryLook {
  const category = categories?.find((entry) => entry.key === key);
  return {
    label: category?.name || translatedLabel(key),
    icon: category?.icon ?? "circle-dashed",
    color: category?.color ?? "gray",
  };
}

/**
 * What a picker offers: the visible categories, plus the current one even
 * when hidden, those of the fitting kind first.
 */
export function pickableCategories(
  categories: CategoryDetails[],
  current: Category | null,
  kind?: CategoryKind,
): CategoryDetails[] {
  const offered = categories.filter((category) => !category.hidden || category.key === current);
  if (!kind) {
    return offered;
  }
  return [
    ...offered.filter((category) => category.kind === kind),
    ...offered.filter((category) => category.kind !== kind),
  ];
}