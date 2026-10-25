import { describe, expect, it } from "vitest";
import { maskAmount } from "@/lib/privacy";

const parts = (locale: string, amount: number) =>
  new Intl.NumberFormat(locale, { style: "currency", currency: "EUR", signDisplay: "exceptZero" }).formatToParts(amount);

describe("maskAmount", () => {
  it("hides figures and sign but keeps the currency where the locale puts it", () => {
    expect(maskAmount(parts("de", -1234.5))).toBe("•••• €");
    expect(maskAmount(parts("en", 1234.5))).toBe("€••••");
  });
});
