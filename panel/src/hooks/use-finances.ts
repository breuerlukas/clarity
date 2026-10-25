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
/**
 * The bank connections with the accounts of the open space. A connection
 * shows wherever one of its accounts is; one without any accounts stays in
 * its own space, so it can be removed there. Hidden accounts only show
 * where they can be shown again.
 */
export function useConnections({ withHidden = false } = {}) {
  const space = useSpace();
  return useQuery({
    queryKey: ["connections"],
    queryFn: api.connections,
    select: (data) =>
      data.connections.flatMap((connection) => {
        const accounts = connection.accounts.filter((account) => account.space === space.id && (withHidden || !account.hidden));
        const empty = connection.space === space.id && connection.accounts.length === 0;
        return accounts.length > 0 || empty ? [{ ...connection, accounts }] : [];
      }),
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

/**
 * The fixed costs that still run; ended ones neither cost nor come up, and
 * ones the owner said are none don't count at all.
 */
export function useRecurringPayments() {
  const space = useSpace();
  return useQuery({
    queryKey: ["recurring", space.id],
    queryFn: () => api.recurring(space.id),
    select: (data) => data.payments.filter((payment) => payment.status !== "ENDED" && !payment.ignored),
  });
}

/** Every fixed cost, ended ones included, for the fixed costs page. */
export function useAllRecurringPayments() {
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
