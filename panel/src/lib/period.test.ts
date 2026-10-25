import { describe, expect, it } from "vitest";
import { customPeriod, parsePeriod, periodMonths, periodRange, shiftPeriod } from "@/lib/period";

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

describe("custom and all", () => {
  it("reads custom days in any order and refuses broken ones", () => {
    expect(parsePeriod("2026-10-25_2026-03-15", "rolling")).toEqual({ kind: "custom", key: "2026-03-15_2026-10-25" });
    expect(parsePeriod("2026-02-31_2026-13-01", "rolling").kind).toBe("rolling");
    expect(parsePeriod("all", "rolling")).toEqual({ kind: "all", key: "all" });
  });

  it("covers exactly the chosen days and the months they touch", () => {
    const period = customPeriod("2026-03-15", "2026-05-02");
    expect(periodRange(period, today)).toEqual({ from: "2026-03-15", to: "2026-05-02" });
    expect(periodMonths(period, today)).toEqual(["2026-03", "2026-04", "2026-05"]);
    expect(periodRange(customPeriod("2026-09-01", "2026-12-31"), today).to).toBe("2026-10-12");
  });

  it("reaches ten years back but charts only from the oldest booking", () => {
    const all = { kind: "all" as const, key: "all" };
    expect(periodRange(all, today)).toEqual({ from: "2016-10-14", to: "2026-10-12" });
    expect(periodMonths(all, today, "2026-08-20")).toEqual(["2026-08", "2026-09", "2026-10"]);
    expect(shiftPeriod(all, -1)).toBe(all);
  });
});
