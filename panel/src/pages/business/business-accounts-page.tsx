import { useState } from "react";
import { useTranslation } from "react-i18next";
import { FileSignature, Plus, ReceiptText, UserRound } from "lucide-react";
import { Button } from "@/components/ui/button";
import { AmountBadge } from "@/components/amount-badge";
import { Notice } from "@/components/notice";
import { PageHeader, Panel } from "@/components/panel";
import { SyncButton } from "@/components/sync-button";
import { useBusinessTransactions, useContracts } from "@/hooks/use-business";
import { useAllConnections, useConnections } from "@/hooks/use-finances";
import { useSpaces } from "@/hooks/use-space";
import type { BusinessTransaction } from "@/lib/api";
import { matchLabel, matchPayment, personalIbans, type PaymentMatch } from "@/lib/business";
import { addDays, isoDate } from "@/lib/dates";
import { formatDay, formatMoney } from "@/lib/format";
import { total } from "@/lib/overview";
import { ConnectBankDialog } from "@/pages/accounts/connect-bank-dialog";
import { ConnectionCard } from "@/pages/accounts/connection-card";
import { AssignPaymentDialog } from "@/pages/business/assign-payment-dialog";

const RECENT_DAYS = 90;

/**
 * The business accounts: connecting them, their balance, and the bookings of
 * the last 90 days with the contract, private withdrawal or deposit each one
 * belongs to. Private means to or from an account in a personal space.
 */
export function BusinessAccountsPage() {
  const { t } = useTranslation();
  const connections = useConnections();
  const spaces = useSpaces();
  const allConnections = useAllConnections();
  const contracts = useContracts();
  const today = isoDate(new Date());
  const transactions = useBusinessTransactions(addDays(today, -RECENT_DAYS), today);
  const [connecting, setConnecting] = useState(false);
  const [selected, setSelected] = useState<BusinessTransaction | null>(null);
  const personalSpaces = new Set(
    (spaces.data ?? []).filter((space) => space.kind === "PERSONAL").map((space) => space.id),
  );
  const ownIbans = personalIbans(
    (allConnections.data ?? [])
      .filter((connection) => personalSpaces.has(connection.space))
      .flatMap((connection) => connection.accounts.map((account) => account.iban)),
  );
  const matchOf = (transaction: BusinessTransaction) => matchPayment(transaction, contracts.data ?? [], ownIbans);
  const accounts = (connections.data ?? []).flatMap((connection) => connection.accounts);
  return (
    <>
      <PageHeader title={t("nav.accounts")} subtitle={t("business.accounts_subtitle")}>
        <SyncButton connections={connections.data} />
        <Button onClick={() => setConnecting(true)}>
          <Plus />
          {t("connect.title_business")}
        </Button>
      </PageHeader>
      {(connections.isError || transactions.isError) && <Notice className="mb-6">{t("common.error")}</Notice>}
      {connections.data?.length === 0 ? (
        <Panel title={t("business.accounts_empty_title")}>
          <p className="max-w-xl text-muted-foreground">{t("business.accounts_empty_text")}</p>
        </Panel>
      ) : (
        <div className="grid items-start gap-6 xl:grid-cols-[minmax(0,1fr)_minmax(0,1.4fr)]">
          <div className="flex min-w-0 flex-col gap-6">
            <section className="grid gap-1 rounded-3xl border bg-card p-6">
              <span className="text-sm text-muted-foreground">{t("business.balance")}</span>
              <span className="figures text-3xl font-extrabold tracking-[-0.02em]">
                {formatMoney(total(accounts.map((account) => account.balance ?? 0)))}
              </span>
            </section>
            {connections.data?.map((connection) => (
              <ConnectionCard
                key={connection.id}
                connection={connection}
                onReconnect={() => setConnecting(true)}
                business
              />
            ))}
          </div>
          <Panel title={t("business.bookings")} aside={t("business.bookings_note", { days: RECENT_DAYS })}>
            {transactions.data?.length === 0 && <p className="text-muted-foreground">{t("transactions.empty")}</p>}
            <ul className="zebra -mx-2" aria-busy={transactions.isPending}>
              {transactions.data?.map((transaction) => (
                <BookingRow
                  key={transaction.id}
                  transaction={transaction}
                  match={matchOf(transaction)}
                  onSelect={() => setSelected(transaction)}
                />
              ))}
            </ul>
          </Panel>
        </div>
      )}
      <ConnectBankDialog open={connecting} onOpenChange={setConnecting} />
      <AssignPaymentDialog
        transaction={selected}
        match={selected ? matchPayment({ ...selected, assignment: null }, contracts.data ?? [], ownIbans) : null}
        contracts={contracts.data ?? []}
        onClose={() => setSelected(null)}
      />
    </>
  );
}

function BookingRow({
  transaction,
  match,
  onSelect,
}: {
  transaction: BusinessTransaction;
  match: PaymentMatch;
  onSelect: () => void;
}) {
  const { t } = useTranslation();
  const Icon = match.kind === "CONTRACT" ? FileSignature : UserRound;
  const unmatched = match.kind === "NONE" && transaction.amount > 0;
  return (
    <li className="zebra-row rounded-md">
      <button
        type="button"
        onClick={onSelect}
        className="grid w-full grid-cols-[1fr_auto] items-center gap-3 rounded-md px-2 py-2 text-left hover:bg-raised"
      >
        <span className="grid min-w-0 gap-1">
          <span className="truncate font-medium">{transaction.counterparty || t("transactions.no_counterparty")}</span>
          <span className="flex min-w-0 flex-wrap items-center gap-x-2 gap-y-1 text-sm text-muted-foreground">
            <span>{formatDay(transaction.date)}</span>
            {match.kind !== "NONE" ? (
              <span className="inline-flex min-w-0 items-center gap-1 rounded-full bg-brand/12 px-2 py-px text-xs font-bold text-brand-text">
                <Icon className="size-3.5 shrink-0" aria-hidden />
                <span className="truncate">{matchLabel(match, t)}</span>
                {!match.automatic && <span className="sr-only">{t("business.assigned_manually")}</span>}
              </span>
            ) : (
              unmatched &&
              transaction.invoices.length === 0 && (
                <span className="rounded-full px-2 py-px text-xs font-bold text-warning ring-1 ring-warning/30 ring-inset">
                  {t("business.unmatched")}
                </span>
              )
            )}
            {transaction.invoices.map((invoice) => (
              <span
                key={invoice.id}
                className="inline-flex min-w-0 items-center gap-1 rounded-full px-2 py-px text-xs font-bold text-foreground ring-1 ring-divider ring-inset"
              >
                <ReceiptText className="size-3.5 shrink-0" aria-hidden />
                <span className="truncate">{t("invoices.pays", { number: invoice.number })}</span>
              </span>
            ))}
          </span>
        </span>
        <AmountBadge
          value={transaction.amount}
          currency={transaction.currency}
          neutral={match.kind === "WITHDRAWAL" || match.kind === "DEPOSIT"}
        />
      </button>
    </li>
  );
}
