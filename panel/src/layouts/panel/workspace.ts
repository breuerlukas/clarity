import { useLocation } from "react-router";

/**
 * The owner's own money or the business. Which one is open follows from the
 * address, so a reload or a shared link lands in the same place.
 */
export type Workspace = "personal" | "business";

export const WORKSPACE_HOME: Record<Workspace, string> = {
  personal: "/",
  business: "/business",
};

export function workspaceOf(pathname: string): Workspace {
  return /^\/business(\/|$)/.test(pathname) ? "business" : "personal";
}

export function useWorkspace(): Workspace {
  return workspaceOf(useLocation().pathname);
}
