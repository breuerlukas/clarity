import { useId, useState, type FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Bell } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { Field } from "@/components/field";
import { LedgerSkeleton } from "@/components/ledger";
import { Notice } from "@/components/notice";
import { api, ApiError, type MailSecurity, type NotificationDraft, type NotificationSettings } from "@/lib/api";
import { SettingRow, SettingsSection } from "@/pages/settings/settings-card";

const DEFAULT_PORT: Record<MailSecurity, number> = { SSL: 465, STARTTLS: 587 };
const EVENTS = ["consent_events", "sync_events", "price_events"] as const;

/**
 * Mails about what happens while Clarity is closed: expiring bank access,
 * failing syncs, price changes. Sent through the owner's own SMTP server,
 * never a third-party push service.
 */
export function NotificationsSection() {
  const { t } = useTranslation();
  const settings = useQuery({ queryKey: ["notifications"], queryFn: api.notifications, select: (data) => data.settings });
  return (
    <SettingsSection icon={Bell} title={t("settings.section_notifications")} description={t("settings.description_notifications")}>
      {settings.isPending && <LedgerSkeleton rows={4} />}
      {settings.isError && <Notice className="py-4">{t("common.error")}</Notice>}
      {settings.data && <NotificationsForm stored={"host" in settings.data ? (settings.data as NotificationSettings) : null} />}
    </SettingsSection>
  );
}

function draftOf(stored: NotificationSettings | null, language: string): NotificationDraft {
  return {
    enabled: stored?.enabled ?? true,
    host: stored?.host ?? "",
    port: stored?.port ?? DEFAULT_PORT.SSL,
    security: stored?.security ?? "SSL",
    username: stored?.username ?? "",
    password: "",
    sender: stored?.sender ?? "",
    recipient: stored?.recipient ?? "",
    language,
    consent_events: stored?.consent_events ?? true,
    sync_events: stored?.sync_events ?? true,
    price_events: stored?.price_events ?? true,
  };
}

function NotificationsForm({ stored }: { stored: NotificationSettings | null }) {
  const { t, i18n } = useTranslation();
  const queryClient = useQueryClient();
  const securityId = useId();
  const [draft, setDraft] = useState(() => draftOf(stored, i18n.resolvedLanguage ?? "de"));
  const set = <K extends keyof NotificationDraft>(key: K) => (value: NotificationDraft[K]) =>
    setDraft((previous) => ({ ...previous, [key]: value }));
  const saved = () => queryClient.invalidateQueries({ queryKey: ["notifications"] });
  const save = useMutation({ mutationFn: () => api.updateNotifications(draft), onSuccess: saved });
  const test = useMutation({
    mutationFn: async () => {
      await api.updateNotifications(draft);
      await api.testNotification();
    },
    onSuccess: saved,
  });
  const setSecurity = (security: MailSecurity) =>
    setDraft((previous) => ({
      ...previous,
      security,
      port: previous.port === DEFAULT_PORT[previous.security] ? DEFAULT_PORT[security] : previous.port,
    }));
  const submit = (event: FormEvent) => {
    event.preventDefault();
    save.mutate();
  };
  const error = save.error ?? test.error;
  return (
    <form onSubmit={submit} className="divide-y divide-divider">
      <SettingRow label={t("notifications.enabled")} hint={t("notifications.enabled_hint")}>
        <Switch checked={draft.enabled} onCheckedChange={set("enabled")} aria-label={t("notifications.enabled")} />
      </SettingRow>
      <div className="grid gap-4 py-4">
        <div className="grid gap-4 sm:grid-cols-[1fr_6rem_9rem]">
          <Field label={t("notifications.host")} value={draft.host} onChange={(event) => set("host")(event.target.value)} placeholder="smtp.example.com" maxLength={253} autoComplete="off" required />
          <Field label={t("notifications.port")} type="number" min={1} max={65535} value={draft.port} onChange={(event) => set("port")(Number(event.target.value))} required />
          <div className="grid content-start gap-2">
            <Label htmlFor={securityId}>{t("notifications.security")}</Label>
            <select
              id={securityId}
              value={draft.security}
              onChange={(event) => setSecurity(event.target.value as MailSecurity)}
              className="h-12 rounded-2xl border-[1.5px] border-transparent bg-ground px-4 text-sm text-foreground outline-none focus-visible:border-primary"
            >
              <option value="SSL">SSL/TLS</option>
              <option value="STARTTLS">STARTTLS</option>
            </select>
          </div>
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label={t("notifications.username")} value={draft.username} onChange={(event) => set("username")(event.target.value)} maxLength={200} autoComplete="off" required />
          <Field
            label={t("notifications.password")}
            hint={stored?.has_password ? t("notifications.password_keep") : undefined}
            type="password"
            value={draft.password}
            onChange={(event) => set("password")(event.target.value)}
            maxLength={500}
            autoComplete="new-password"
            required={!stored?.has_password}
          />
          <Field label={t("notifications.sender")} type="email" value={draft.sender} onChange={(event) => set("sender")(event.target.value)} maxLength={254} required />
          <Field label={t("notifications.recipient")} type="email" value={draft.recipient} onChange={(event) => set("recipient")(event.target.value)} maxLength={254} required />
        </div>
      </div>
      {EVENTS.map((event) => (
        <SettingRow key={event} label={t(`notifications.${event}`)} hint={t(`notifications.${event}_hint`)}>
          <Switch checked={draft[event]} onCheckedChange={set(event)} aria-label={t(`notifications.${event}`)} />
        </SettingRow>
      ))}
      <div className="grid gap-3 py-4">
        {error && (
          <Notice>{t(`notifications.errors.${error instanceof ApiError ? error.code : ""}`, { defaultValue: t("common.error") })}</Notice>
        )}
        {test.isSuccess && <p className="text-sm text-positive">{t("notifications.test_sent", { recipient: draft.recipient })}</p>}
        <div className="flex flex-wrap justify-end gap-2">
          <Button type="button" variant="outline" loading={test.isPending} disabled={save.isPending} onClick={() => test.mutate()}>
            {t("notifications.test")}
          </Button>
          <Button type="submit" loading={save.isPending} disabled={test.isPending}>
            {t("common.save")}
          </Button>
        </div>
      </div>
    </form>
  );
}
