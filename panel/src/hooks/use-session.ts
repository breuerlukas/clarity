import { useQuery } from "@tanstack/react-query";
import { api, ApiError } from "@/lib/api";

export function useSetupStatus() {
  return useQuery({
    queryKey: ["setup"],
    queryFn: api.setupStatus,
    staleTime: Infinity,
  });
}

/** Resolves to null instead of failing when nobody is signed in. */
export function useSession() {
  return useQuery({
    queryKey: ["session"],
    queryFn: async () => {
      try {
        return await api.session();
      } catch (error) {
        if (error instanceof ApiError && error.status === 401) {
          return null;
        }
        throw error;
      }
    },
    staleTime: 5 * 60_000,
  });
}