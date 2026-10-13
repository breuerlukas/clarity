import { useQuery } from "@tanstack/react-query";
import { useSpace } from "@/hooks/use-space";
import { api } from "@/lib/api";
import { isoDate, monthRange } from "@/lib/dates";
import { lastMonths } from "@/lib/statistics";

/** Every space's connections, for what has to look across spaces. */
export function useAllConnections() {
  return useQuery({
    queryKey: ["connections"],
    queryFn: api.connections,
    select: (data) => data.connections,
  });
}

/** The open space's connections. */
export function useConnections() {
  const space = useSpace();
  return useQuery({
    queryKey: ["connections"],
    queryFn: api.connections,
    select: (data) => data.connections.filter((connection) => connection.space === space.id),
  });
}

export function useTransactionsBetween(from: string, to: string) {
  const space = useSpace();
  return useQuery({
    queryKey: ["transactions", space.id, from, to],
    queryFn: () => api.transactions(space.id, from, to),
    select: (data) => data.transactions,
  });
}

export function useRecurringPayments() {
  const space = useSpace();
  return useQuery({
    queryKey: ["recurring", space.id],
    queryFn: () => api.recurring(space.id),
    select: (data) => data.payments,
  });
}

export const HISTORY_MONTHS = 12;

/**
 * The last twelve months including the current one, shared by the balance
 * chart on the overview and the statistics page.
 */
export function useHistory() {
  const space = useSpace();
  const months = lastMonths(HISTORY_MONTHS);
  const from = monthRange(months[0]).from;
  const to = isoDate(new Date());
  return useQuery({
    queryKey: ["transactions", space.id, "history", from, to],
    queryFn: () => api.transactions(space.id, from, to),
    select: (data) => data.transactions,
  });
}
