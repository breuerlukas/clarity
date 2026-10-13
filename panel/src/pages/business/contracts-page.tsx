import { useState } from "react";
import { useSearchParams } from "react-router";
import { useTranslation } from "react-i18next";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Pencil, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Notice } from "@/components/notice";
import { PageHeader, Panel, PanelSkeleton } from "@/components/panel";
import { Segmented } from "@/components/segmented";
import { useContracts } from "@/hooks/use-business";
import { api, type Contract } from "@/lib/api";
import {
  contractStatus,
  contractsByCustomer,
  nextExit,
  nextRenewal,
  type ContractStatus,
} from "@/lib/business";
import { daysBetween, isoDate } from "@/lib/dates";
import { formatDate, formatMoney } from "@/lib/format";
import { cn } from "@/lib/utils";
import { ContractDialog } from "@/pages/business/contract-dialog";
import { EndContractDialog } from "@/pages/business/end-contract-dialog";
import { SOON_DAYS } from "@/pages/business/contract-events";

type Filter = "current" | "ended" | "all";

const shows: Record<Filter, (status: ContractStatus) => boolean> = {
  current: (status) => status !== "ended",
  ended: (status) => status === "ended",
  all: () => true,
};

/**
 * Every contract, grouped by customer with what the customer brings in a
 * month. New contracts start here, and each one can be changed or ended.
 */
export function ContractsPage() {
  const { t } = useTranslation();
  const contracts = useContracts();
  const [params, setParams] = useSearchParams();
  const [filter, setFilter] = useState<Filter>("current");
  const [editing, setEditing] = useState<Contract | null>(null);
  const [ending, setEnding] = useState<Contract | null>(null);
  const creating = params.get("new") === "1";
  const today = isoDate(new Date());
  const setCreating = (open: boolean) =>
    setParams(
      (previous) => {
        const updated = new URLSearchParams(previous);
        if (open) {
          updated.set("new", "1");
        } else {
          updated.delete("new");
        }
        return updated;
      },
      { replace: true },
    );
  const groups = contractsByCustomer(contracts.data ?? [], today)
    .map((group) => ({
      ...group,
      contracts: group.contracts.filter((contract) => shows[filter](contractStatus(contract, today))),
    }))
    .filter((group) => group.contracts.length > 0);
  const customers = [...new Set((contracts.data ?? []).map((contract) => contract.customer))].sort();
  return (
    <>
      <PageHeader title={t("nav.contracts")}>
        <Segmented<Filter>
          label={t("subscriptions.filter")}
          value={filter}
          options={(["current", "ended", "all"] as const).map((value) => ({
            value,
            label: t(`business.filter_${value}`),
          }))}
          onChange={setFilter}
        />
        <Button onClick={() => setCreating(true)}>
          <Plus />
          {t("business.add_contract")}
        </Button>
      </PageHeader>
      {contracts.isError && <Notice className="mb-6">{t("common.error")}</Notice>}
      {contracts.data ? (
        <div className="grid items-start gap-6 xl:grid-cols-2">
          {groups.length === 0 && (
            <Panel title={t("business.contracts_empty_title")}>
              <p className="max-w-xl text-muted-foreground">{t("business.contracts_empty_text")}</p>
            </Panel>
          )}
          {groups.map((group) => (
            <Panel
              key={group.customer}
              title={group.customer}
              aside={group.monthly > 0 ? t("business.per_month", { amount: formatMoney(group.monthly) }) : undefined}
            >
              <ul className="zebra -mx-2">
                {group.contracts.map((contract) => (
                  <ContractRow
                    key={contract.id}
                    contract={contract}
                    today={today}
                    onEdit={() => setEditing(contract)}
                    onEnd={() => setEnding(contract)}
                  />
                ))}
              </ul>
            </Panel>
          ))}
        </div>
      ) : (
        <PanelSkeleton className="h-96" />
      )}
      <ContractDialog
        open={creating || editing !== null}
        contract={editing}
        customers={customers}
        onOpenChange={(open) => {
          if (!open) {
            setCreating(false);
            setEditing(null);
          }
        }}
      />
      <EndContractDialog contract={ending} onClose={() => setEnding(null)} />
    </>
  );
}

function ContractRow({
  contract,
  today,
  onEdit,
  onEnd,
}: {
  contract: Contract;
  today: string;
  onEdit: () => void;
  onEnd: () => void;
}) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const resume = useMutation({
    mutationFn: () => api.resumeContract(contract.id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["contracts"] }),
  });
  const status = contractStatus(contract, today);
  return (
    <li className="zebra-row grid gap-1.5 rounded-xl px-2 py-3">
      <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
        <span className="figures font-bold">
          {formatMoney(contract.amount)} <span className="font-normal text-muted-foreground">{t(`interval.${contract.interval}`)}</span>
        </span>
        <StatusBadge status={status} />
        <span className="ml-auto flex items-center gap-1">
          {status === "active" || status === "planned" ? (
            <Button size="sm" variant="outline" onClick={onEnd}>
              {t("business.end")}
            </Button>
          ) : (
            <Button size="sm" variant="outline" disabled={resume.isPending} onClick={() => resume.mutate()}>
              {t("business.resume")}
            </Button>
          )}
          <Button size="icon-sm" variant="ghost" onClick={onEdit} aria-label={t("business.edit_contract")}>
            <Pencil />
          </Button>
        </span>
      </div>
      <span className="text-sm text-muted-foreground">
        {t(status === "planned" ? "business.terms_planned" : "business.terms", {
          start: formatDate(contract.start_date),
          term: t("business.months", { count: contract.term_months }),
          notice: t("business.months", { count: contract.notice_months }),
        })}
      </span>
      <NextDates contract={contract} status={status} today={today} />
      {resume.isError && <Notice className="text-sm">{t("common.error")}</Notice>}
    </li>
  );
}

/** The date that matters next, in warning color when it is close. */
function NextDates({ contract, status, today }: { contract: Contract; status: ContractStatus; today: string }) {
  const { t } = useTranslation();
  if (status === "ended" || status === "cancelled") {
    const soon = status === "cancelled" && daysBetween(today, contract.end_date!) <= SOON_DAYS;
    return (
      <span className={cn("text-sm", soon ? "font-bold text-warning" : "text-muted-foreground")}>
        {t(status === "ended" ? "business.ended_on" : "business.ends_on", { date: formatDate(contract.end_date!) })}
      </span>
    );
  }
  const exit = nextExit(contract, today);
  const soon = daysBetween(today, exit.deadline) <= SOON_DAYS;
  return (
    <span className={cn("text-sm", soon ? "font-bold text-warning" : "text-muted-foreground")}>
      {t("business.renews", {
        renewal: formatDate(nextRenewal(contract, today)),
        deadline: formatDate(exit.deadline),
        end: formatDate(exit.end),
      })}
    </span>
  );
}

function StatusBadge({ status }: { status: ContractStatus }) {
  const { t } = useTranslation();
  return (
    <span
      className={cn(
        "rounded-full px-2 py-px text-xs font-bold",
        status === "active" && "bg-positive/12 text-positive",
        status === "planned" && "text-muted-foreground ring-1 ring-divider ring-inset",
        status === "cancelled" && "bg-warning/12 text-warning",
        status === "ended" && "bg-raised-strong text-muted-foreground",
      )}
    >
      {t(`business.status_${status}`)}
    </span>
  );
}
