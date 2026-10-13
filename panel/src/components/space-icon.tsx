import { createElement } from "react";
import { spaceIcon } from "@/lib/space-icons";

/** The icon a space was given, by its stored name. */
export function SpaceIcon({ icon, className }: { icon: string; className?: string }) {
  return createElement(spaceIcon(icon), { className, "aria-hidden": true });
}
