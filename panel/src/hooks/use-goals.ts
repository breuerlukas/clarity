import { useQuery } from "@tanstack/react-query";
import { useSpace } from "@/hooks/use-space";
import { api } from "@/lib/api";

/** The open space's savings goals, oldest first. */
export function useGoals() {
  const space = useSpace();
  return useQuery({
    queryKey: ["goals", space.id],
    queryFn: () => api.goals(space.id),
    select: (data) => data.goals,
  });
}
