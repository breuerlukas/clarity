import { describe, expect, it } from "vitest";
import type { CategoryDetails } from "@/lib/api";
import { categoryLook, pickableCategories } from "@/lib/categories";

const category = (key: string, overrides: Partial<CategoryDetails> = {}): CategoryDetails => ({
  key,
  name: null,
  icon: "circle-dashed",
  color: "gray",
  kind: "SPENDING",
  keywords: [],
  hidden: false,
  built_in: true,
  system: false,
  ...overrides,
});

const translated = (key: string) => `t:${key}`;

describe("categoryLook", () => {
  it("prefers the owner's name over the translated label", () => {
    const categories = [category("GROCERIES", { name: "Supermarkt", icon: "store", color: "green" })];
    expect(categoryLook(categories, "GROCERIES", translated)).toEqual({ label: "Supermarkt", icon: "store", color: "green" });
  });

  it("falls back to the translated label for unnamed and unknown keys", () => {
    expect(categoryLook([category("DINING")], "DINING", translated).label).toBe("t:DINING");
    expect(categoryLook(undefined, "gone", translated)).toEqual({ label: "t:gone", icon: "circle-dashed", color: "gray" });
  });
});

describe("pickableCategories", () => {
  const categories = [
    category("SALARY", { kind: "INCOME" }),
    category("GROCERIES"),
    category("TRAVEL", { hidden: true }),
    category("pets", { built_in: false }),
  ];

  it("leaves hidden categories out unless one is the current", () => {
    expect(pickableCategories(categories, null).map((entry) => entry.key)).toEqual(["SALARY", "GROCERIES", "pets"]);
    expect(pickableCategories(categories, "TRAVEL").map((entry) => entry.key)).toContain("TRAVEL");
  });

  it("offers the fitting kind first", () => {
    expect(pickableCategories(categories, null, "SPENDING").map((entry) => entry.key)).toEqual(["GROCERIES", "pets", "SALARY"]);
  });
});