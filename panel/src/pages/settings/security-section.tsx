import { useTranslation } from "react-i18next";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Monitor, ShieldCheck, Smartphone } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Notice } from "@/components/notice";
import { LedgerSkeleton } from "@/components/ledger";
import { api, type OwnerSession } from "@/lib/api";
import { formatRelative } from "@/lib/format";
import { SettingRow, SettingsSection } from "@/pages/settings/settings-card";

/** Every place the owner is signed in, each with its own way out. */
export function SecuritySection() {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const sessions = useQuery({
    queryKey: ["owner-sessions"],
    queryFn: api.sessions,
    select: (data) => data.sessions,
  });
  const refresh = () => queryClient.invalidateQueries({ queryKey: ["owner-sessions"] });
  const closeOthers = useMutation({ mutationFn: api.closeOtherSessions, onSuccess: refresh });
  const others = sessions.data?.filter((session) => !session.current).length ?? 0;
  return (
    <SettingsSection
      icon={ShieldCheck}
      title={t("settings.section_security")}
      description={t("settings.description_security")}
    >
      <SettingRow label={t("settings.two_factor")} hint={t("settings.two_factor_hint")}>
        <span className="inline-flex h-8 items-center gap-1.5 rounded-full bg-brand/12 px-3 text-[13px] font-bold text-brand-text">
          <ShieldCheck className="size-4" aria-hidden />
          {t("settings.two_factor_active")}
        </span>
      </SettingRow>
      <SettingRow label={t("settings.sessions")} hint={t("settings.sessions_hint")}>
        <Button
          variant="outline"
          disabled={others === 0 || closeOthers.isPending}
          onClick={() => closeOthers.mutate()}
        >
          {t("settings.sessions_close_others")}
        </Button>
      </SettingRow>
      {sessions.isError && <Notice className="py-4">{t("common.error")}</Notice>}
      {sessions.isPending && <LedgerSkeleton rows={2} />}
      <ul className="divide-y divide-divider">
        {sessions.data?.map((session) => (
          <SessionRow key={session.id} session={session} onClosed={refresh} />
        ))}
      </ul>
    </SettingsSection>
  );
}

function SessionRow({
  session,
  onClosed,
}: {
  session: OwnerSession;
  onClosed: () => void;
}) {
  const { t } = useTranslation();
  const close = useMutation({
    mutationFn: () => api.closeSession(session.id),
    onSuccess: onClosed,
  });
  const Icon = session.mobile ? Smartphone : Monitor;
  const device =
    [session.browser, session.platform].filter(Boolean).join(t("settings.session_on")) ||
    t("settings.session_unknown");
  return (
    <li className="flex flex-wrap items-center gap-3 py-3.5">
      <span className="grid size-10 shrink-0 place-items-center rounded-full bg-raised text-muted-foreground">
        <Icon className="size-[18px]" aria-hidden />
      </span>
      <span className="min-w-0 flex-1">
        <span className="flex flex-wrap items-center gap-2">
          <b className="text-[15px]">{device}</b>
          {session.current && (
            <span className="rounded-full bg-brand/12 px-2 py-0.5 text-xs font-bold text-brand-text">
              {t("settings.session_current")}
            </span>
          )}
        </span>
        <span className="block text-xs text-muted-foreground">
          {t("settings.session_details", {
            address: session.ip_address,
            time: formatRelative(session.last_seen_at),
          })}
        </span>
      </span>
      {!session.current && (
        <Button variant="ghost" size="sm" disabled={close.isPending} onClick={() => close.mutate()}>
          {t("settings.session_close")}
        </Button>
      )}
    </li>
  );
}
