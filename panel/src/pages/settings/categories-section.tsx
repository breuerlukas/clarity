import { useState } from "react";
import { useTranslation } from "react-i18next";
import { ChevronRight, EyeOff, Plus, Shapes } from "lucide-react";
import { Button } from "@/components/ui/button";
import { CategoryIcon } from "@/components/category-icon";
import { LedgerSkeleton } from "@/components/ledger";
import { useCategories, useCategoryLook } from "@/hooks/use-categories";
import type { CategoryDetails, CategoryKind } from "@/lib/api";
import { cn } from "@/lib/utils";
import { CategoryForm } from "@/pages/settings/category-form";
import { SettingRow, SettingsSection } from "@/pages/settings/settings-card";

const KINDS: CategoryKind[] = ["SPENDING", "INCOME"];

/**
 * Every category by kind, hidden ones dimmed; choosing one, or adding a new
 * one, swaps the list for its form.
 */
export function CategoriesSection() {
  const { t } = useTranslation();
  const categories = useCategories();
  const look = useCategoryLook();
  const [editing, setEditing] = useState<CategoryDetails | "new" | null>(null);
  if (editing) {
    return (
      <CategoryForm
        key={editing === "new" ? "new" : editing.key}
        category={editing === "new" ? null : editing}
        onDone={() => setEditing(null)}
      />
    );
  }
  return (
    <SettingsSection
      icon={Shapes}
      title={t("settings.section_categories")}
      description={t("settings.description_categories")}
    >
      <SettingRow label={t("categories.own")} hint={t("categories.own_hint")}>
        <Button onClick={() => setEditing("new")}>
          <Plus />
          {t("categories.create")}
        </Button>
      </SettingRow>
      {categories.isPending && <LedgerSkeleton rows={4} />}
      {KINDS.map((kind) => (
        <div key={kind} className="py-4">
          <h3 className="mb-2 text-sm font-bold text-label">{t(`categories.kind_${kind}`)}</h3>
          <ul className="grid gap-1 sm:grid-cols-2">
            {categories.data
              ?.filter((category) => category.kind === kind)
              .map((category) => (
                <li key={category.key}>
                  <button
                    type="button"
                    onClick={() => setEditing(category)}
                    className="flex w-full items-center gap-3 rounded-xl px-2.5 py-2 text-left text-sm transition-colors hover:bg-raised"
                  >
                    <CategoryIcon category={category.key} className={cn("size-8", category.hidden && "opacity-50")} />
                    <span className={cn("min-w-0 flex-1 truncate", category.hidden && "text-muted-foreground")}>
                      {look(category.key).label}
                    </span>
                    {category.hidden && (
                      <EyeOff className="size-4 shrink-0 text-muted-foreground" aria-label={t("categories.hidden")} />
                    )}
                    <ChevronRight className="size-4 shrink-0 text-muted-foreground" aria-hidden />
                  </button>
                </li>
              ))}
          </ul>
        </div>
      ))}
    </SettingsSection>
  );
}