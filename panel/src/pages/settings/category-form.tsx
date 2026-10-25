import { useEffect, useRef, useState, type FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { ArrowDownLeft, ArrowLeft, ArrowUpRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Switch } from "@/components/ui/switch";
import { CategoryBadge } from "@/components/category-icon";
import { Field } from "@/components/field";
import { ConfirmDelete } from "@/components/confirm-delete";
import { Notice } from "@/components/notice";
import { Segmented } from "@/components/segmented";
import { useBudgets, useCategories, useCategoryLook } from "@/hooks/use-categories";
import { useSpace } from "@/hooks/use-space";
import { api, ApiError, type Category, type CategoryDetails, type CategoryKind } from "@/lib/api";
import { CATEGORY_COLORS, CATEGORY_ICONS, categoryColorStyle } from "@/lib/category-icons";
import { cn } from "@/lib/utils";

/** Never counted as spending, so a budget on them would stay empty. */
const UNBUDGETED = new Set(["CASH", "TRANSFER"]);

/**
 * Creates a category or changes one. A built-in one keeps its translated
 * label while the name stays empty and can go back to how it shipped; an own
 * one can be deleted once the owner chose where its transactions move. It
 * opens at its top, wherever the list was scrolled to. The budget belongs
 * to the open space and is saved while the category is a spending one: before
 * it turns into income, after it turns into spending.
 */
export function CategoryForm({ category, onDone }: { category: CategoryDetails | null; onDone: () => void }) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const categories = useCategories();
  const look = useCategoryLook();
  const space = useSpace();
  const budgets = useBudgets();
  const [name, setName] = useState(category?.name ?? "");
  const [kind, setKind] = useState<CategoryKind>(category?.kind ?? "SPENDING");
  const [icon, setIcon] = useState(category?.icon ?? "circle-dashed");
  const [color, setColor] = useState(category?.color ?? "gray");
  const [keywords, setKeywords] = useState(category?.keywords.join("\n") ?? "");
  const [hidden, setHidden] = useState(category?.hidden ?? false);
  const [target, setTarget] = useState<string | null>(null);
  const [limit, setLimit] = useState<string | null>(null);
  const form = useRef<HTMLFormElement>(null);
  useEffect(() => {
    form.current?.scrollIntoView({ block: "start" });
  }, []);
  const finish = async (moved: boolean) => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ["categories"] }),
      queryClient.invalidateQueries({ queryKey: ["budgets"] }),
      ...(moved ? [queryClient.invalidateQueries({ queryKey: ["transactions"] }), queryClient.invalidateQueries({ queryKey: ["recurring"] })] : []),
    ]);
    onDone();
  };
  const draft = { name: name.trim() || null, icon, color, kind, keywords: keywords.split("\n").filter((line) => line.trim()) };
  const budgetable = space.kind === "PERSONAL" && kind === "SPENDING" && !UNBUDGETED.has(category?.key ?? "");
  const currentLimit = budgets.data?.find((budget) => budget.category === category?.key)?.limit ?? null;
  const wantedLimit = kind === "INCOME" ? null : limit === null ? currentLimit : limit.trim() ? Number(limit) : null;
  const saveBudget = (key: Category) =>
    wantedLimit === currentLimit ? Promise.resolve() : api.updateBudget(space.id, key, wantedLimit);
  const save = useMutation({
    mutationFn: async () => {
      if (!category) {
        return saveBudget((await api.createCategory(draft)).category.key);
      }
      if (category.kind === "SPENDING") {
        await saveBudget(category.key);
      }
      await api.updateCategory(category.key, draft, hidden);
      return category.kind === "SPENDING" ? undefined : saveBudget(category.key);
    },
    onSuccess: () => finish(false),
  });
  const reset = useMutation({ mutationFn: () => api.resetCategory(category!.key), onSuccess: () => finish(false) });
  const remove = useMutation({ mutationFn: () => api.deleteCategory(category!.key, target!), onSuccess: () => finish(true) });
  const submit = (event: FormEvent) => {
    event.preventDefault();
    save.mutate();
  };
  const error = save.error ?? reset.error ?? remove.error;
  const errorCode = error instanceof ApiError ? error.code : undefined;
  const nameRequired = !category?.built_in;
  return (
    <form ref={form} onSubmit={submit} className="grid scroll-mt-7 gap-5">
      <div className="flex items-center gap-3 pr-8">
        <Button type="button" variant="ghost" size="icon" onClick={onDone} aria-label={t("categories.back")}>
          <ArrowLeft />
        </Button>
        <CategoryBadge icon={icon} color={color} className="size-11 [&_svg]:size-5" />
        <h2 className="min-w-0 truncate text-xl font-extrabold">
          {category ? name.trim() || look(category.key).label : t("categories.create")}
        </h2>
      </div>
      <Field
        label={t("categories.name")}
        value={name}
        onChange={(event) => setName(event.target.value)}
        maxLength={40}
        required={nameRequired}
        placeholder={category?.built_in ? t(`category.${category.key}`) : t("categories.name_placeholder")}
        hint={category?.built_in ? t("categories.name_hint") : undefined}
        autoComplete="off"
      />
      {!category?.system && (
        <div className="grid gap-2">
          <span className="text-sm font-bold text-label">{t("categories.kind")}</span>
          <Segmented<CategoryKind>
            label={t("categories.kind")}
            value={kind}
            options={[
              { value: "SPENDING", label: t("categories.kind_SPENDING"), icon: ArrowUpRight },
              { value: "INCOME", label: t("categories.kind_INCOME"), icon: ArrowDownLeft },
            ]}
            onChange={setKind}
          />
        </div>
      )}
      {budgetable && (
        <Field
          label={t("categories.budget")}
          type="number"
          inputMode="decimal"
          min="0.01"
          step="0.01"
          value={limit ?? currentLimit ?? ""}
          onChange={(event) => setLimit(event.target.value)}
          placeholder={t("categories.budget_placeholder")}
          hint={t("categories.budget_hint")}
        />
      )}
      <fieldset className="grid gap-2">
        <legend className="mb-2 text-sm font-bold text-label">{t("categories.icon")}</legend>
        <div className="grid max-w-md grid-cols-8 gap-1.5">
          {CATEGORY_ICONS.map((option) => (
            <button
              key={option.id}
              type="button"
              aria-label={option.id}
              aria-pressed={icon === option.id}
              onClick={() => setIcon(option.id)}
              className={cn(
                "grid aspect-square place-items-center rounded-xl ring-1 ring-divider transition-colors hover:bg-raised",
                icon === option.id && "bg-brand/12 text-nav-active-text ring-brand/40",
              )}
            >
              <option.icon className="size-4" aria-hidden />
            </button>
          ))}
        </div>
      </fieldset>
      <fieldset className="grid gap-2">
        <legend className="mb-2 text-sm font-bold text-label">{t("categories.color")}</legend>
        <div className="flex flex-wrap gap-2">
          {CATEGORY_COLORS.map((option) => (
            <button
              key={option}
              type="button"
              aria-label={t(`categories.color_${option}`)}
              aria-pressed={color === option}
              onClick={() => setColor(option)}
              className={cn(
                "size-9 rounded-full bg-raised-strong ring-offset-2 ring-offset-card transition-shadow",
                color === option ? "ring-2 ring-primary" : "ring-1 ring-divider hover:ring-2",
              )}
              style={categoryColorStyle(option) && { backgroundColor: `var(--category-${option})` }}
            />
          ))}
        </div>
      </fieldset>
      <label className="grid gap-2">
        <span className="text-sm font-bold text-label">{t("categories.keywords")}</span>
        <textarea
          value={keywords}
          onChange={(event) => setKeywords(event.target.value)}
          rows={4}
          placeholder={t("categories.keywords_placeholder")}
          className="min-h-24 w-full rounded-2xl border-[1.5px] border-transparent bg-ground px-4 py-3 text-sm text-foreground outline-none placeholder:text-placeholder focus-visible:border-primary"
        />
        <span className="text-sm text-muted-foreground">{t("categories.keywords_hint")}</span>
      </label>
      {category && !category.system && (
        <label className="flex items-center justify-between gap-4 rounded-2xl bg-raised px-4 py-3 text-sm">
          <span>{t("categories.hide")}</span>
          <Switch checked={hidden} onCheckedChange={setHidden} />
        </label>
      )}
      {category && !category.built_in && target !== null && (
        <label className="grid gap-2 rounded-2xl bg-raised p-4 text-sm">
          <span className="font-bold">{t("categories.delete_target")}</span>
          <select
            value={target}
            onChange={(event) => setTarget(event.target.value)}
            className="h-12 rounded-2xl border-[1.5px] border-transparent bg-ground px-4 text-sm text-foreground outline-none focus-visible:border-primary"
          >
            {categories.data
              ?.filter((option) => option.key !== category.key)
              .map((option) => (
                <option key={option.key} value={option.key}>
                  {look(option.key).label}
                </option>
              ))}
          </select>
        </label>
      )}
      {error && <Notice>{t(`categories.errors.${errorCode}`, { defaultValue: t("common.error") })}</Notice>}
      <div className="flex flex-wrap items-center justify-end gap-2">
        {category?.built_in && (
          <Button type="button" variant="link" className="mr-auto px-0 text-muted-foreground" disabled={reset.isPending} onClick={() => reset.mutate()}>
            {t("categories.reset")}
          </Button>
        )}
        {category && !category.built_in && target === null && (
          <Button type="button" variant="link" className="mr-auto px-0 text-muted-foreground" onClick={() => setTarget("OTHER")}>
            {t("categories.delete")}
          </Button>
        )}
        {category && !category.built_in && target !== null && (
          <ConfirmDelete
            label={t("categories.delete_confirm")}
            name={name.trim() || look(category.key).label}
            consequence={t("categories.delete_consequence", { target: look(target).label })}
            pending={remove.isPending}
            error={remove.error}
            errors="categories"
            onConfirm={() => remove.mutate()}
          />
        )}
        <Button type="button" variant="outline" onClick={onDone}>
          {t("common.cancel")}
        </Button>
        <Button type="submit" loading={save.isPending} disabled={nameRequired && !name.trim()}>
          {t(category ? "common.save" : "categories.create_submit")}
        </Button>
      </div>
    </form>
  );
}