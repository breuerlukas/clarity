import { describe, expect, it } from "vitest";
import { activeUrl } from "@/layouts/panel/active-url";

describe("activeUrl", () => {
  const urls = ["/", "/transactions", "/settings"];

  it("lights the overview only on the root", () => {
    expect(activeUrl("/", urls)).toBe("/");
    expect(activeUrl("/transactions", urls)).toBe("/transactions");
  });

  it("lights a section for its sub pages", () => {
    expect(activeUrl("/settings/security", urls)).toBe("/settings");
    expect(activeUrl("/unknown", urls)).toBeUndefined();
  });
});
