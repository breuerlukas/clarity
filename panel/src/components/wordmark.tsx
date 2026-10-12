import { cn } from "@/lib/utils";

/** The mark (three stripes on a green tile) and the name. */
export function Wordmark({ className }: { className?: string }) {
  return (
    <span className={cn("inline-flex items-center gap-2.5", className)}>
      <span className="grid size-7 place-items-center rounded-[9px] bg-primary">
        <svg viewBox="0 0 20 20" aria-hidden="true" className="size-4">
          <rect x="1" y="3" width="18" height="3.4" rx="1.4" className="fill-on-brand" />
          <rect x="1" y="8.3" width="18" height="3.4" rx="1.4" className="fill-on-brand opacity-60" />
          <rect x="1" y="13.6" width="18" height="3.4" rx="1.4" className="fill-on-brand" />
        </svg>
      </span>
      <b className="text-base tracking-tight text-foreground">Clarity</b>
    </span>
  );
}