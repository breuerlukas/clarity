import { useTranslation } from "react-i18next";
import type { InvoiceStatus } from "@/lib/api";
import { cn } from "@/lib/utils";

export function InvoiceStatusBadge({ status }: { status: InvoiceStatus }) {
  const { t } = useTranslation();
  return (
    <span
      className={cn(
        "rounded-full px-2 py-px text-xs font-bold whitespace-nowrap",
        status === "PAID" && "bg-positive/12 text-positive",
        status === "OVERDUE" && "bg-warning/12 text-warning",
        status === "PARTIAL" && "text-warning ring-1 ring-warning/30 ring-inset",
        status === "ANNOUNCED" && "bg-brand/12 text-brand-text",
        status === "OPEN" && "text-muted-foreground ring-1 ring-divider ring-inset",
      )}
    >
      {t(`invoices.status_${status}`)}
    </span>
  );
}

/** Marks an invoice read from a plain PDF or a scan that nobody confirmed. */
export function ReviewBadge() {
  const { t } = useTranslation();
  return (
    <span className="rounded-full bg-raised-strong px-2 py-px text-xs font-bold whitespace-nowrap text-foreground">
      {t("invoices.review")}
    </span>
  );
}
