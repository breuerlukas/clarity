import { useTranslation } from "react-i18next";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { RefreshCw } from "lucide-react";
import { Button } from "@/components/ui/button";
import { api, type Connection } from "@/lib/api";
import { formatRelative } from "@/lib/format";

/**
 * When the accounts were last read, and a button to read them now. Every
 * account is read either way; the connections only decide what it shows.
 */
export function SyncButton({ connections }: { connections?: Connection[] }) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const synchronize = useMutation({
    mutationFn: api.synchronize,
    onSuccess: () => queryClient.invalidateQueries(),
  });
  const lastSynced = Math.max(
    0,
    ...(connections ?? []).flatMap((connection) =>
      connection.accounts.map((account) => account.last_synced_at),
    ),
  );
  if (!connections?.length) {
    return null;
  }
  return (
    <div className="flex items-center gap-3">
      <span className="text-sm text-muted-foreground max-sm:sr-only" aria-live="polite">
        {synchronize.data?.started === false
          ? t("accounts.sync_busy")
          : lastSynced > 0
            ? t("accounts.last_synced", { time: formatRelative(lastSynced) })
            : t("accounts.never_synced")}
      </span>
      <Button
        variant="outline"
        onClick={() => synchronize.mutate()}
        disabled={synchronize.isPending}
      >
        <RefreshCw className={synchronize.isPending ? "animate-spin" : ""} />
        {synchronize.isPending ? t("accounts.syncing") : t("accounts.sync")}
      </Button>
    </div>
  );
}