import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Notice } from "@/components/notice";
import { PageHeader, Panel, PanelSkeleton } from "@/components/panel";
import { useConnections } from "@/hooks/use-finances";
import { useGoals } from "@/hooks/use-goals";
import type { SavingsGoal } from "@/lib/api";
import { DepositDialog } from "@/pages/goals/deposit-dialog";
import { GoalCard } from "@/pages/goals/goal-card";
import { GoalDialog } from "@/pages/goals/goal-dialog";

/**
 * What the owner saves up for, each with its progress. Money set aside by
 * hand comes off the spending room on the overview.
 */
export function GoalsPage() {
  const { t } = useTranslation();
  const goals = useGoals();
  const connections = useConnections();
  const accounts = connections.data?.flatMap((connection) => connection.accounts) ?? [];
  const [editing, setEditing] = useState<{ goal: SavingsGoal | null } | null>(null);
  const [depositing, setDepositing] = useState<SavingsGoal | null>(null);
  return (
    <>
      <PageHeader title={t("goals.title")}>
        <Button onClick={() => setEditing({ goal: null })}>
          <Plus />
          {t("goals.create")}
        </Button>
      </PageHeader>
      {goals.isError && <Notice className="mb-6">{t("common.error")}</Notice>}
      {goals.isPending && <PanelSkeleton className="h-56" />}
      {goals.data?.length === 0 && (
        <Panel>
          <p className="text-muted-foreground">{t("goals.empty")}</p>
        </Panel>
      )}
      <div className="grid gap-6 md:grid-cols-2 2xl:grid-cols-3">
        {goals.data?.map((goal) => (
          <GoalCard
            key={goal.id}
            goal={goal}
            accounts={accounts}
            onEdit={() => setEditing({ goal })}
            onDeposit={() => setDepositing(goal)}
          />
        ))}
      </div>
      <GoalDialog
        open={editing !== null}
        goal={editing?.goal ?? null}
        accounts={accounts}
        onClose={() => setEditing(null)}
      />
      <DepositDialog goal={depositing} onClose={() => setDepositing(null)} />
    </>
  );
}
