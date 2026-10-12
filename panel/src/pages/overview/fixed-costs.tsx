import { Trans, useTranslation } from "react-i18next";
import { Panel } from "@/components/panel";
import { CategoryIcon } from "@/components/category-icon";
import type { Account, RecurringPayment } from "@/lib/api";
import { currentMonth, isoDate } from "@/lib/dates";
import { formatDate, formatMoney } from "@/lib/format";
import { total, upcomingThisMonth } from "@/lib/overview";

/**
 * The detected fixed costs, led by the one sentence that matters most in the
 * middle of a month: what is still due, and what is left after it.
 */
export function FixedCosts({
  payments,
  accounts,
}: {
  payments?: RecurringPayment[];
  accounts?: Account[];
}) {
  const { t } = useTranslation();
  const monthly = payments ? total(payments.map((payment) => payment.monthly_amount)) : 0;
  return (
    <Panel
      title={t("overview.fixed")}
      aside={
        payments?.length ? (
          <span className="figures">
            {t("overview.fixed_total", { amount: formatMoney(monthly) })}
          </span>
        ) : undefined
      }
    >
      {payments && accounts && <Forecast payments={payments} accounts={accounts} />}
      {payments?.length === 0 && (
        <p className="text-muted-foreground">{t("overview.fixed_empty")}</p>
      )}
      <ul className="zebra -mx-2">
        {payments?.map((payment) => (
          <li
            key={`${payment.counterparty}-${payment.cycle}`}
            className="zebra-row grid grid-cols-[auto_1fr_auto] items-center gap-3 rounded-md px-2 py-2"
          >
            <CategoryIcon category={payment.category} />
            <span className="grid min-w-0 gap-0.5">
              <span className="truncate font-medium">{payment.counterparty}</span>
              <span className="truncate text-sm text-muted-foreground">
                {t(`cycle.${payment.cycle}`)},{" "}
                {t("overview.fixed_next", { date: formatDate(payment.next_date) })}
              </span>
            </span>
            <span className="figures font-medium">
              {formatMoney(payment.amount, payment.currency)}
            </span>
          </li>
        ))}
      </ul>
    </Panel>
  );
}

function Forecast({
  payments,
  accounts,
}: {
  payments: RecurringPayment[];
  accounts: Account[];
}) {
  const { t } = useTranslation();
  const balance = total(accounts.map((account) => account.balance ?? 0));
  const upcoming = total(
    upcomingThisMonth(payments, isoDate(new Date()), currentMonth()).map(
      (payment) => payment.amount,
    ),
  );
  return (
    <p className="rounded-md bg-raised px-4 py-3 text-[15px] leading-relaxed ring-1 ring-line">
      {upcoming > 0 ? (
        <Trans
          i18nKey="overview.forecast"
          values={{
            amount: formatMoney(upcoming),
            rest: formatMoney(balance - upcoming),
          }}
          components={{ strong: <strong className="figures font-semibold" /> }}
        />
      ) : (
        t("overview.forecast_none")
      )}
    </p>
  );
}