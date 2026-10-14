import { useState } from "react";
import { useTranslation } from "react-i18next";
import { ArrowDownLeft, ArrowUpRight } from "lucide-react";
import { Notice } from "@/components/notice";
import { PageHeader, Panel } from "@/components/panel";
import { Segmented } from "@/components/segmented";
import { ExpectedActualChart, type ExpectedActualSide } from "@/components/charts/expected-actual-chart";
import { useInvoices } from "@/hooks/use-business";
import { HISTORY_MONTHS } from "@/hooks/use-finances";
import type { Invoice, InvoiceDirection } from "@/lib/api";
import { daysBetween, isoDate } from "@/lib/dates";
import { formatDate, formatMoney } from "@/lib/format";
import {
  counterpartyOf,
  expectedVersusActual,
  isOpen,
  openSummary,
  sortInvoices,
  type OpenSummary,
} from "@/lib/invoices";
import { lastMonths } from "@/lib/statistics";
import { cn } from "@/lib/utils";
import { InvoiceDialog } from "@/pages/business/invoice-dialog";
import { InvoiceStatusBadge, ReviewBadge } from "@/pages/business/invoice-status";
import { MailboxCard } from "@/pages/business/mailbox-card";

type Filter = "open" | "paid" | "all";

const shows: Record<Filter, (invoice: Invoice) => boolean> = {
  open: isOpen,
  paid: (invoice) => !isOpen(invoice),
  all: () => true,
};

/**
 * The invoices the mailboxes brought in, sent and received: what is still
 * open and overdue, how payments compare with the invoices month by month,
 * and each invoice with the bookings that paid it.
 */
export function InvoicesPage() {
  const { t } = useTranslation();
  const invoices = useInvoices();
  const [direction, setDirection] = useState<InvoiceDirection>("OUTGOING");
  const [filter, setFilter] = useState<Filter>("open");
  const [side, setSide] = useState<ExpectedActualSide>("income");
  const [selected, setSelected] = useState<string | null>(null);
  const list = sortInvoices(
    (invoices.data ?? []).filter((invoice) => invoice.direction === direction && shows[filter](invoice)),
  );
  const history = expectedVersusActual(invoices.data ?? [], lastMonths(HISTORY_MONTHS));
  const open = invoices.data?.find((invoice) => invoice.id === selected) ?? null;
  return (
    <>
      <PageHeader title={t("nav.invoices")} subtitle={t("invoices.subtitle")} />
      {invoices.isError && <Notice className="mb-6">{t("common.error")}</Notice>}
      {invoices.data ? (
        <div className="grid gap-6">
          <OpenFigures summary={openSummary(invoices.data)} />
          <div className="grid items-start gap-6 xl:grid-cols-[minmax(0,1.6fr)_minmax(0,1fr)]">
            <div className="flex min-w-0 flex-col gap-6">
              <Panel
                title={t("invoices.list")}
                aside={
                  <Segmented<InvoiceDirection>
                    label={t("invoices.direction")}
                    value={direction}
                    options={[
                      { value: "OUTGOING", label: t("invoices.direction_OUTGOING"), icon: ArrowUpRight },
                      { value: "INCOMING", label: t("invoices.direction_INCOMING"), icon: ArrowDownLeft },
                    ]}
                    onChange={setDirection}
                  />
                }
              >
                <Segmented<Filter>
                  label={t("subscriptions.filter")}
                  value={filter}
                  options={(["open", "paid", "all"] as const).map((value) => ({
                    value,
                    label: t(`invoices.filter_${value}`),
                  }))}
                  onChange={setFilter}
                />
                {list.length === 0 && (
                  <p className="text-muted-foreground">
                    {invoices.data.length === 0 ? t("invoices.empty") : t("invoices.empty_filter")}
                  </p>
                )}
                <ul className="zebra -mx-2">
                  {list.map((invoice) => (
                    <InvoiceRow key={invoice.id} invoice={invoice} onSelect={() => setSelected(invoice.id)} />
                  ))}
                </ul>
              </Panel>
              <Panel
                title={t("invoices.expected_actual")}
                aside={
                  <Segmented<ExpectedActualSide>
                    label={t("invoices.expected_actual")}
                    value={side}
                    options={[
                      { value: "income", label: t("invoices.income") },
                      { value: "expenses", label: t("invoices.expenses") },
                    ]}
                    onChange={setSide}
                  />
                }
              >
                <ExpectedActualChart
                  data={history}
                  side={side}
                  height={260}
                  labels={{
                    expected: t("invoices.expected"),
                    actual: t("invoices.actual"),
                    difference: t("invoices.difference"),
                  }}
                />
                <p className="text-sm text-muted-foreground">{t(`invoices.expected_actual_${side}`)}</p>
              </Panel>
            </div>
            <MailboxCard />
          </div>
        </div>
      ) : (
        <div className="h-96 rounded-3xl border bg-card" aria-busy="true" />
      )}
      <InvoiceDialog invoice={open} onClose={() => setSelected(null)} />
    </>
  );
}

/** Open receivables and payables, what of them is overdue, and what needs a look. */
function OpenFigures({ summary }: { summary: OpenSummary }) {
  const { t } = useTranslation();
  const figures = [
    {
      label: t("invoices.receivables"),
      value: formatMoney(summary.receivables),
      note: summary.overdueReceivables > 0 ? t("invoices.overdue_amount", { amount: formatMoney(summary.overdueReceivables) }) : null,
    },
    {
      label: t("invoices.payables"),
      value: formatMoney(summary.payables),
      note: summary.overduePayables > 0 ? t("invoices.overdue_amount", { amount: formatMoney(summary.overduePayables) }) : null,
    },
    { label: t("invoices.overdue"), value: String(summary.overdueCount), note: null, warn: summary.overdueCount > 0 },
    { label: t("invoices.to_review"), value: String(summary.reviewCount), note: null },
  ];
  return (
    <section className="overflow-hidden rounded-3xl border bg-card">
      <dl className="grid grid-cols-2 gap-px bg-divider lg:grid-cols-4">
        {figures.map((figure) => (
          <div key={figure.label} className="grid content-start gap-1 bg-card p-5 sm:p-6">
            <dt className="text-sm text-muted-foreground">{figure.label}</dt>
            <dd
              className={cn(
                "figures text-[clamp(1.25rem,4.5vw,1.75rem)] leading-tight font-extrabold",
                figure.warn && "text-warning",
              )}
            >
              {figure.value}
            </dd>
            {figure.note && <dd className="text-sm font-bold text-warning">{figure.note}</dd>}
          </div>
        ))}
      </dl>
    </section>
  );
}

function InvoiceRow({ invoice, onSelect }: { invoice: Invoice; onSelect: () => void }) {
  const { t } = useTranslation();
  const today = isoDate(new Date());
  const due = invoice.due_date;
  return (
    <li className="zebra-row rounded-md">
      <button
        type="button"
        onClick={onSelect}
        className="grid w-full grid-cols-[1fr_auto] items-center gap-3 rounded-md px-2 py-2.5 text-left hover:bg-raised"
      >
        <span className="grid min-w-0 gap-1">
          <span className="flex min-w-0 items-center gap-2">
            <span className="truncate font-medium">{counterpartyOf(invoice) || t("invoices.no_counterparty")}</span>
            {invoice.needs_review && <ReviewBadge />}
          </span>
          <span className="flex min-w-0 flex-wrap items-center gap-x-2 gap-y-1 text-sm text-muted-foreground">
            <span className="truncate">{invoice.number || t("invoices.no_number")}</span>
            <span aria-hidden>·</span>
            <span className={cn(invoice.status === "OVERDUE" && "font-bold text-warning")}>
              {isOpen(invoice) && due
                ? invoice.status === "OVERDUE"
                  ? t("invoices.overdue_since", { count: daysBetween(due, today) })
                  : t("invoices.due_on", { date: formatDate(due) })
                : formatDate(invoice.invoice_date)}
            </span>
            <InvoiceStatusBadge status={invoice.status} />
          </span>
        </span>
        <span className="grid justify-items-end gap-0.5 text-right">
          <span className="figures font-bold">{formatMoney(invoice.gross_amount, invoice.currency)}</span>
          {invoice.status === "PARTIAL" && (
            <span className="figures text-xs text-muted-foreground">
              {t("invoices.open_amount", { amount: formatMoney(invoice.open_amount, invoice.currency) })}
            </span>
          )}
        </span>
      </button>
    </li>
  );
}
