import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";

export function useContracts() {
  return useQuery({
    queryKey: ["contracts"],
    queryFn: api.contracts,
    select: (data) => data.contracts,
  });
}

export function useBusinessConnections() {
  return useQuery({
    queryKey: ["business-connections"],
    queryFn: api.businessConnections,
    select: (data) => data.connections,
  });
}

export function useBusinessTransactions(from: string, to: string) {
  return useQuery({
    queryKey: ["business-transactions", from, to],
    queryFn: () => api.businessTransactions(from, to),
    select: (data) => data.transactions,
  });
}
