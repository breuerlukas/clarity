import { describe, expect, it } from "vitest";
import { accountLabel } from "@/lib/format";

describe("accountLabel", () => {
  it("puts the bank before the account name", () => {
    expect(accountLabel("ING", { name: "Girokonto", iban: null })).toBe("ING, Girokonto");
  });

  it("falls back to the masked IBAN without a name", () => {
    expect(accountLabel("ING", { name: "", iban: "DE89370400440532013000" })).toBe("ING, DE89 •••• 3000");
  });
});