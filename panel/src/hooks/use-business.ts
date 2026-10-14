import { useQuery } from "@tanstack/react-query";
import { useSpace } from "@/hooks/use-space";
import { api } from "@/lib/api";

export function useContracts() {
  const space = useSpace();
  return useQuery({
    queryKey: ["contracts", space.id],
    queryFn: () => api.contracts(space.id),
    select: (data) => data.contracts,
  });
}

export function useBusinessTransactions(from: string, to: string) {
  const space = useSpace();
  return useQuery({
    queryKey: ["business-transactions", space.id, from, to],
    queryFn: () => api.businessTransactions(space.id, from, to),
    select: (data) => data.transactions,
  });
}

export function useInvoices() {
  const space = useSpace();
  return useQuery({
    queryKey: ["invoices", space.id],
    queryFn: () => api.invoices(space.id),
    select: (data) => data.invoices,
  });
}

export function useMailboxes() {
  const space = useSpace();
  return useQuery({
    queryKey: ["mailboxes", space.id],
    queryFn: () => api.mailboxes(space.id),
    select: (data) => data.mailboxes,
  });
}

/** The bookings that could pay an invoice, fetched while its dialog is open. */
export function usePaymentSuggestions(invoice: string | null) {
  return useQuery({
    queryKey: ["payment-suggestions", invoice],
    queryFn: () => api.paymentSuggestions(invoice!),
    select: (data) => data.suggestions,
    enabled: invoice !== null,
  });
}
