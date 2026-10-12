import { describe, expect, it } from "vitest";
import { parsePeriod, periodMonths, periodRange, shiftPeriod } from "@/lib/period";

const today = new Date(2026, 9, 12);

describe("period", () => {
  it("reads every kind from the url and falls back to the current one", () => {
    expect(parsePeriod("2026-Q4", "month")).toEqual({ kind: "quarter", key: "2026-Q4" });
    expect(parsePeriod("2025", "month")).toEqual({ kind: "year", key: "2025" });
    expect(parsePeriod("12m", "month").kind).toBe("rolling");
    expect(parsePeriod("nonsense", "year").kind).toBe("year");
  });

  it("crosses year boundaries when shifting", () => {
    expect(shiftPeriod({ kind: "quarter", key: "2026-Q1" }, -1).key).toBe("2025-Q4");
    expect(shiftPeriod({ kind: "month", key: "2026-01" }, -1).key).toBe("2025-12");
  });

  it("lists the months and stops the range at today", () => {
    expect(periodMonths({ kind: "quarter", key: "2026-Q4" })).toEqual(["2026-10", "2026-11", "2026-12"]);
    expect(periodMonths({ kind: "rolling", key: "12m" }, today)[0]).toBe("2025-11");
    expect(periodRange({ kind: "year", key: "2026" }, today)).toEqual({ from: "2026-01-01", to: "2026-10-12" });
  });
});
