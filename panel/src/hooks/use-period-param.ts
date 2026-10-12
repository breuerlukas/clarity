import { useSearchParams } from "react-router";
import { currentPeriod, parsePeriod, type Period, type PeriodKind } from "@/lib/period";

/** The selected period lives in the URL (?period=2026-Q4), so links keep it. */
export function usePeriodParam(fallback: PeriodKind): [Period, (period: Period) => void] {
  const [params, setParams] = useSearchParams();
  const period = parsePeriod(params.get("period"), fallback);
  const setPeriod = (next: Period) =>
    setParams(
      (previous) => {
        const updated = new URLSearchParams(previous);
        if (next.key === currentPeriod(fallback).key) {
          updated.delete("period");
        } else {
          updated.set("period", next.key);
        }
        return updated;
      },
      { replace: true },
    );
  return [period, setPeriod];
}
