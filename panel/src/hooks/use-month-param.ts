import { useSearchParams } from "react-router";
import { currentMonth, isValidMonth, type MonthKey } from "@/lib/dates";

/** The selected month lives in the URL, so reload and back keep it. */
export function useMonthParam(): [MonthKey, (month: MonthKey) => void] {
  const [params, setParams] = useSearchParams();
  const value = params.get("month");
  const month = isValidMonth(value) ? value : currentMonth();
  const setMonth = (next: MonthKey) =>
    setParams(
      (previous) => {
        const updated = new URLSearchParams(previous);
        if (next === currentMonth()) {
          updated.delete("month");
        } else {
          updated.set("month", next);
        }
        return updated;
      },
      { replace: true },
    );
  return [month, setMonth];
}