import { useId } from "react";
import { cn } from "@/lib/utils";

/**
 * Small flags drawn as SVG: emoji flags show up as plain letters on
 * Windows.
 */
export function Flag({ country, className }: { country: "de" | "gb"; className?: string }) {
  const id = useId();
  const classes = cn("h-3 w-[18px] shrink-0 overflow-hidden rounded-[2px] ring-1 ring-black/10", className);
  if (country === "de") {
    return (
      <svg viewBox="0 0 5 3" className={classes} aria-hidden="true">
        <rect width="5" height="1" fill="#000" />
        <rect y="1" width="5" height="1" fill="#dd0000" />
        <rect y="2" width="5" height="1" fill="#ffce00" />
      </svg>
    );
  }
  return (
    <svg viewBox="0 0 60 30" className={classes} aria-hidden="true">
      <clipPath id={`${id}-sheet`}>
        <path d="M0,0 v30 h60 v-30 z" />
      </clipPath>
      <clipPath id={`${id}-diagonals`}>
        <path d="M30,15 h30 v15 z v15 h-30 z h-30 v-15 z v-15 h30 z" />
      </clipPath>
      <g clipPath={`url(#${id}-sheet)`}>
        <path d="M0,0 v30 h60 v-30 z" fill="#012169" />
        <path d="M0,0 L60,30 M60,0 L0,30" stroke="#fff" strokeWidth="6" />
        <path d="M0,0 L60,30 M60,0 L0,30" clipPath={`url(#${id}-diagonals)`} stroke="#c8102e" strokeWidth="4" />
        <path d="M30,0 v30 M0,15 h60" stroke="#fff" strokeWidth="10" />
        <path d="M30,0 v30 M0,15 h60" stroke="#c8102e" strokeWidth="6" />
      </g>
    </svg>
  );
}
