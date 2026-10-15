import { useCallback } from "react";
import { useTranslation } from "react-i18next";
import { useQuery } from "@tanstack/react-query";
import { api, type Category } from "@/lib/api";
import { categoryLook } from "@/lib/categories";

/** Every category, hidden ones included, in their order. */
export function useCategories() {
  return useQuery({
    queryKey: ["categories"],
    queryFn: api.categories,
    select: (data) => data.categories,
  });
}

/** Name, icon and color of a category by its key. */
export function useCategoryLook() {
  const { t } = useTranslation();
  const categories = useCategories();
  return useCallback(
    (key: Category) =>
      categoryLook(categories.data, key, (unknown) =>
        t(`category.${unknown}`, { defaultValue: t("category.OTHER") }),
      ),
    [categories.data, t],
  );
}