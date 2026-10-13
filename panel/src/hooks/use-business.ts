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
