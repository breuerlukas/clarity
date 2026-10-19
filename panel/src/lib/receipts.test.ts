import { describe, expect, it } from "vitest";
import { isSplit, splitFromItems } from "@/lib/receipts";

describe("splitFromItems", () => {
  it("sums items per category and leaves the rest in the main category", () => {
    const parts = splitFromItems(-7.36, "GROCERIES", [
      { name: "Bananen", amount: 1.99, category: "GROCERIES" },
      { name: "Zahnpasta", amount: 2.45, category: "HEALTH" },
      { name: "Pfand", amount: -0.25, category: "GROCERIES" },
    ]);
    expect(parts).toEqual([
      { category: "GROCERIES", amount: -4.91 },
      { category: "HEALTH", amount: -2.45 },
    ]);
    expect(isSplit(parts, -7.36)).toBe(true);
  });

  it("needs no split when everything is in one category", () => {
    const parts = splitFromItems(-3, "DINING", [{ name: "Kaffee", amount: 3, category: "DINING" }]);
    expect(isSplit(parts, -3)).toBe(false);
  });

  it("refuses items that come to more than was paid", () => {
    const parts = splitFromItems(-5, "GROCERIES", [{ name: "Wein", amount: 6, category: "LEISURE" }]);
    expect(isSplit(parts, -5)).toBe(false);
  });
});