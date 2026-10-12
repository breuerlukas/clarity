import type { ReactNode } from "react";

export function ChartTooltip({
  title,
  rows,
}: {
  title: string;
  rows: { label: string; value: string; swatch?: string }[];
}) {
  return (
    <div className="grid min-w-44 gap-1.5 rounded-md border border-line bg-card px-3 py-2.5 text-sm text-foreground shadow-[0_8px_24px_rgb(0_0_0/0.12)]">
      <span className="font-semibold">{title}</span>
      {rows.map((row) => (
        <span key={row.label} className="flex items-center justify-between gap-4">
          <span className="flex items-center gap-2 text-muted-foreground">
            {row.swatch && <Swatch color={row.swatch} />}
            {row.label}
          </span>
          <span className="figures font-medium">{row.value}</span>
        </span>
      ))}
    </div>
  );
}

export function Swatch({ color }: { color: string }) {
  return (
    <span
      className="inline-block size-2.5 shrink-0 rounded-[3px]"
      style={{ backgroundColor: color }}
      aria-hidden="true"
    />
  );
}

export function Legend({ children }: { children: ReactNode }) {
  return <div className="flex flex-wrap gap-x-5 gap-y-1 text-sm text-muted-foreground">{children}</div>;
}