import { useState, type FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Check, UserRound } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Notice } from "@/components/notice";
import { useSession } from "@/hooks/use-session";
import { api, ApiError } from "@/lib/api";
import { SettingRow, SettingsSection } from "@/pages/settings/settings-card";

export function AccountSection() {
  const { t } = useTranslation();
  return (
    <SettingsSection
      icon={UserRound}
      title={t("settings.section_account")}
      description={t("settings.description_account")}
    >
      <NameRow />
      <PasswordForm />
    </SettingsSection>
  );
}

function NameRow() {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const session = useSession();
  /** What the owner typed; until then the field shows the current name. */
  const [draft, setDraft] = useState<string | null>(null);
  const name = draft ?? session.data?.name ?? "";
  const rename = useMutation({
    mutationFn: () => api.rename(name.trim()),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["session"] });
      setDraft(null);
    },
  });
  const submit = (event: FormEvent) => {
    event.preventDefault();
    rename.mutate();
  };
  const unchanged = name.trim() === (session.data?.name ?? "") || !name.trim();
  return (
    <SettingRow label={t("settings.name")} hint={t("settings.name_hint")}>
      <form onSubmit={submit} className="flex w-full gap-2 sm:w-auto">
        <Input
          value={name}
          onChange={(event) => setDraft(event.target.value)}
          maxLength={64}
          autoComplete="username"
          aria-label={t("settings.name")}
          className="w-full sm:w-56"
        />
        <Button type="submit" loading={rename.isPending} disabled={unchanged}>
          {rename.isSuccess && unchanged ? <Check /> : null}
          {t("common.save")}
        </Button>
      </form>
    </SettingRow>
  );
}

function PasswordForm() {
  const { t } = useTranslation();
  const [current, setCurrent] = useState("");
  const [next, setNext] = useState("");
  const [repeat, setRepeat] = useState("");
  const change = useMutation({
    mutationFn: () => api.changePassword(current, next),
    onSuccess: () => {
      setCurrent("");
      setNext("");
      setRepeat("");
    },
  });
  const mismatch = repeat.length > 0 && repeat !== next;
  const submit = (event: FormEvent) => {
    event.preventDefault();
    if (!mismatch) {
      change.mutate();
    }
  };
  const errorCode = change.error instanceof ApiError ? change.error.code : undefined;
  return (
    <form onSubmit={submit} className="flex flex-col gap-4 py-4">
      <div>
        <b className="block text-[15px] font-bold">{t("settings.password")}</b>
        <span className="text-xs text-muted-foreground">{t("settings.password_hint")}</span>
      </div>
      <div className="grid gap-3 sm:grid-cols-3">
        <PasswordField label={t("settings.password_current")} value={current} onChange={setCurrent} autoComplete="current-password" />
        <PasswordField label={t("settings.password_new")} value={next} onChange={setNext} autoComplete="new-password" minLength={12} />
        <PasswordField label={t("settings.password_repeat")} value={repeat} onChange={setRepeat} autoComplete="new-password" minLength={12} />
      </div>
      {mismatch && <Notice>{t("settings.password_mismatch")}</Notice>}
      {change.isError && (
        <Notice>{t(`settings.password_errors.${errorCode}`, { defaultValue: t("common.error") })}</Notice>
      )}
      <div className="flex flex-wrap items-center gap-3">
        <span className="flex-1 text-[13px] text-muted-foreground" aria-live="polite">
          {change.isSuccess ? t("settings.password_changed") : ""}
        </span>
        <Button type="submit" loading={change.isPending} disabled={!current || next.length < 12 || mismatch}>
          {t("settings.password_change")}
        </Button>
      </div>
    </form>
  );
}

function PasswordField({
  label,
  value,
  onChange,
  autoComplete,
  minLength,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  autoComplete: string;
  minLength?: number;
}) {
  return (
    <label className="grid gap-1.5 text-xs font-bold text-label">
      {label}
      <Input
        type="password"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        autoComplete={autoComplete}
        minLength={minLength}
        maxLength={256}
        required
      />
    </label>
  );
}
