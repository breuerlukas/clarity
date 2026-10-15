import { useTranslation } from "react-i18next";
import { useCategories, useCategoryLook } from "@/hooks/use-categories";
import { pickableCategories } from "@/lib/categories";
import type { Category } from "@/lib/api";

/** The native select: on phones it opens the system picker. */
export function CategorySelect({
  value,
  onChange,
}: {
  value: Category | null;
  onChange: (category: Category | null) => void;
}) {
  const { t } = useTranslation();
  const categories = useCategories();
  const look = useCategoryLook();
  return (
    <label className="shrink-0">
      <span className="sr-only">{t("transactions.all_categories")}</span>
      <select
        value={value ?? ""}
        onChange={(event) => onChange(event.target.value || null)}
        className="h-12 max-w-44 rounded-2xl border-[1.5px] border-transparent bg-ground px-4 text-sm text-foreground outline-none focus-visible:border-primary"
      >
        <option value="">{t("transactions.all_categories")}</option>
        {pickableCategories(categories.data ?? [], value).map((category) => (
          <option key={category.key} value={category.key}>
            {look(category.key).label}
          </option>
        ))}
      </select>
    </label>
  );
}