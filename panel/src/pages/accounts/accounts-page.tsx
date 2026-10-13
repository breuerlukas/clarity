import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Notice } from "@/components/notice";
import { PageHeader, Panel, PanelSkeleton } from "@/components/panel";
import { SyncButton } from "@/components/sync-button";
import { useConnections } from "@/hooks/use-finances";
import { ConnectionCard } from "@/pages/accounts/connection-card";
import { ConnectBankDialog } from "@/pages/accounts/connect-bank-dialog";

export function AccountsPage() {
  const { t } = useTranslation();
  const connections = useConnections();
  const [connecting, setConnecting] = useState(false);
  return (
    <>
      <PageHeader title={t("accounts.title")}>
        <SyncButton />
        <Button onClick={() => setConnecting(true)}>
          <Plus />
          {t("accounts.connect")}
        </Button>
      </PageHeader>
      {connections.isError && <Notice className="mb-6">{t("common.error")}</Notice>}
      <div className="grid items-start gap-6 xl:grid-cols-2">
        {connections.isPending && (
          <>
            <PanelSkeleton className="h-64" />
            <PanelSkeleton className="h-64" />
          </>
        )}
        {connections.data?.length === 0 && (
          <Panel title={t("accounts.empty_title")}>
            <p className="max-w-xl text-muted-foreground">{t("accounts.empty_text")}</p>
          </Panel>
        )}
        {connections.data?.map((connection) => (
          <ConnectionCard
            key={connection.id}
            connection={connection}
            onReconnect={() => setConnecting(true)}
          />
        ))}
      </div>
      <ConnectBankDialog open={connecting} onOpenChange={setConnecting} />
    </>
  );
}