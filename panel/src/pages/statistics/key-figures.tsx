import { useTranslation } from "react-i18next";
import type { RecurringPayment } from "@/lib/api";
import { formatMoney, formatPercent } from "@/lib/format";
import { total } from "@/lib/overview";
import { monthlyAverages, type MonthStatistic } from "@/lib/statistics";

/**
 * Four averages over the complete months of the range. One strip divided by
 * rules rather than four separate cards: they belong together.
 */
export function KeyFigures({
  statistics,
  payments,
}: {
  statistics: MonthStatistic[];
  payments?: RecurringPayment[];
}) {
  const { t } = useTranslation();
  const averages = monthlyAverages(statistics);
  const fixed = payments ? total(payments.map((payment) => payment.monthly_amount)) : null;
  const fixedShare =
    fixed !== null && averages.spending ? fixed / averages.spending : null;
  const figures = [
    {
      label: t("statistics.average_in"),
      value: averages.income === null ? "—" : formatMoney(averages.income),
    },
    {
      label: t("statistics.average_out"),
      value: averages.spending === null ? "—" : formatMoney(averages.spending),
    },
    {
      label: t("statistics.average_savings"),
      value: averages.savingsRate === null ? "—" : formatPercent(averages.savingsRate),
      tone:
        averages.savingsRate === null
          ? undefined
          : averages.savingsRate >= 0
            ? "text-positive"
            : "text-warning",
    },
    {
      label: t("statistics.fixed_costs"),
      value: fixed === null ? "—" : formatMoney(fixed),
      note:
        fixedShare === null
          ? undefined
          : t("statistics.fixed_share", { share: formatPercent(fixedShare) }),
    },
  ];
  return (
    <section className="overflow-hidden rounded-lg border border-line bg-card">
      <dl className="grid grid-cols-2 gap-px bg-line lg:grid-cols-4">
        {figures.map((figure) => (
          <div key={figure.label} className="grid content-start gap-1 bg-card p-5 sm:p-6">
            <dt className="text-sm text-muted-foreground">{figure.label}</dt>
            <dd className={`figures text-[clamp(1.25rem,4.5vw,1.75rem)] leading-tight font-semibold tracking-[-0.02em] ${figure.tone ?? ""}`}>
              {figure.value}
            </dd>
            {figure.note && <dd className="text-sm text-muted-foreground">{figure.note}</dd>}
          </div>
        ))}
      </dl>
      <p className="border-t border-line px-5 py-2.5 text-xs text-muted-foreground sm:px-6">
        {t("statistics.averages_note", { count: averages.months })}
      </p>
    </section>
  );
}