import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { isoDate, monthRange } from "@/lib/dates";
import { lastMonths } from "@/lib/statistics";

export function useConnections() {
  return useQuery({
    queryKey: ["connections"],
    queryFn: api.connections,
    select: (data) => data.connections,
  });
}

export function useTransactionsBetween(from: string, to: string) {
  return useQuery({
    queryKey: ["transactions", from, to],
    queryFn: () => api.transactions(from, to),
    select: (data) => data.transactions,
  });
}

export function useRecurringPayments() {
  return useQuery({
    queryKey: ["recurring"],
    queryFn: api.recurring,
    select: (data) => data.payments,
  });
}

export const HISTORY_MONTHS = 12;

/**
 * The last twelve months including the current one, shared by the balance
 * chart on the overview and the statistics page.
 */
export function useHistory() {
  const months = lastMonths(HISTORY_MONTHS);
  const from = monthRange(months[0]).from;
  const to = isoDate(new Date());
  return useQuery({
    queryKey: ["transactions", "history", from, to],
    queryFn: () => api.transactions(from, to),
    select: (data) => data.transactions,
  });
}
