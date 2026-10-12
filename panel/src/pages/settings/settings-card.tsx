import type { ReactNode } from "react";
import type { LucideIcon } from "lucide-react";

/** A settings section: round icon, title and one-line description, then its rows. */
export function SettingsSection({
  icon: Icon,
  title,
  description,
  children,
}: {
  icon: LucideIcon;
  title: string;
  description: string;
  children: ReactNode;
}) {
  return (
    <section className="flex flex-col">
      <div className="flex items-center gap-3.5 pr-8">
        <span className="grid size-11 shrink-0 place-items-center rounded-full bg-brand/12 text-brand">
          <Icon className="size-5" aria-hidden />
        </span>
        <span className="min-w-0">
          <h2 className="text-xl leading-tight font-extrabold">{title}</h2>
          <p className="text-sm text-muted-foreground">{description}</p>
        </span>
      </div>
      <div className="mt-3 divide-y divide-divider border-t border-divider">{children}</div>
    </section>
  );
}

/** One row: label and hint at the left, the control at the right. */
export function SettingRow({
  label,
  hint,
  children,
}: {
  label: string;
  hint?: string;
  children: ReactNode;
}) {
  return (
    <div className="flex min-h-[76px] flex-wrap items-center justify-between gap-x-4 gap-y-3 py-4">
      <span className="min-w-0">
        <b className="block text-[15px] font-bold">{label}</b>
        {hint && <span className="text-xs text-muted-foreground">{hint}</span>}
      </span>
      <div className="flex shrink-0 flex-wrap items-center gap-2">{children}</div>
    </div>
  );
}
