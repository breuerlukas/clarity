import { useMutation, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";

/**
 * Forgets the session (which sends the route guard to the login) and every
 * cached finance query. clear() would detach the guard's own session
 * observer, which then never noticed the logout.
 */
export function useLogout() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: api.logout,
    onSettled: () => {
      queryClient.setQueryData(["session"], null);
      queryClient.removeQueries({
        predicate: (query) => !["session", "setup"].includes(String(query.queryKey[0])),
      });
    },
  });
}
