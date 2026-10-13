import { createContext, useContext } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { api, type Space } from "@/lib/api";

export function useSpaces() {
  return useQuery({
    queryKey: ["spaces"],
    queryFn: api.spaces,
    select: (data) => data.spaces,
  });
}

export const SpaceContext = createContext<Space | null>(null);

/** The space the address points at; only inside a space's pages. */
export function useSpace(): Space {
  const space = useContext(SpaceContext);
  if (!space) {
    throw new Error("useSpace outside of a space");
  }
  return space;
}

/** A path inside a space: spacePath(space, "/accounts") is "/s/home/accounts". */
export function spacePath(space: Pick<Space, "slug">, path = ""): string {
  return `/s/${space.slug}${path}`;
}

/** A path inside the open space. */
export function useSpacePath(): (path?: string) => string {
  const space = useSpace();
  return (path = "") => spacePath(space, path);
}

/** The first space is unnamed until the owner names it. */
export function useSpaceName(): (space: Space) => string {
  const { t } = useTranslation();
  return (space) => space.name || t("spaces.default_name");
}

const LAST_SPACE = "clarity-space";

export function rememberSpace(slug: string) {
  try {
    localStorage.setItem(LAST_SPACE, slug);
  } catch {
    // Without storage the overview of the first space opens next time.
  }
}

export function lastSpace(): string | null {
  try {
    return localStorage.getItem(LAST_SPACE);
  } catch {
    return null;
  }
}
