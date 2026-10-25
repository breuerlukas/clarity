import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useSpace } from "@/hooks/use-space";
import { api } from "@/lib/api";

/** The open space's cash wallet with its balance. */
export function useCashWallet() {
  const space = useSpace();
  return useQuery({
    queryKey: ["cash", space.id],
    queryFn: () => api.cashWallet(space.id),
    select: (data) => data.wallet,
  });
}

/** The open space's receipts, newest first. */
export function useReceipts() {
  const space = useSpace();
  return useQuery({
    queryKey: ["receipts", space.id],
    queryFn: () => api.receipts(space.id),
    select: (data) => data.receipts,
  });
}

/** Reloads everything a cash entry or a receipt shows up in. */
export function useRefreshCash() {
  const queryClient = useQueryClient();
  return () =>
    Promise.all(
      ["transactions", "cash", "receipts", "recurring"].map((key) =>
        queryClient.invalidateQueries({ queryKey: [key] }),
      ),
    );
}
