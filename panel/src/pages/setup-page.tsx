import { useState, type FormEvent } from "react";
import { Navigate, useNavigate } from "react-router";
import { useTranslation } from "react-i18next";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { AuthLayout } from "@/layouts/auth-layout";
import { Field } from "@/components/field";
import { Notice } from "@/components/notice";
import { Splash } from "@/components/splash";
import { Button } from "@/components/ui/button";
import { useSetupStatus } from "@/hooks/use-session";
import { api, ApiError, type SetupAnswer } from "@/lib/api";

export function SetupPage() {
  const { t } = useTranslation();
  const setup = useSetupStatus();
  const [setupCode, setSetupCode] = useState("");
  const [answer, setAnswer] = useState<SetupAnswer | null>(null);
  if (setup.isPending) {
    return <Splash />;
  }
  if (!setup.data?.setup_required) {
    return <Navigate to="/login" replace />;
  }
  return (
    <AuthLayout>
      <p className="text-sm font-medium text-muted-foreground">
        {t("setup.step", { step: answer ? 2 : 1 })}
      </p>
      {answer ? (
        <AuthenticatorStep setupCode={setupCode} answer={answer} />
      ) : (
        <AccountStep
          setupCode={setupCode}
          onSetupCodeChange={setSetupCode}
          onCreated={setAnswer}
        />
      )}
    </AuthLayout>
  );
}

function AccountStep({
  setupCode,
  onSetupCodeChange,
  onCreated,
}: {
  setupCode: string;
  onSetupCodeChange: (code: string) => void;
  onCreated: (answer: SetupAnswer) => void;
}) {
  const { t } = useTranslation();
  const [name, setName] = useState("");
  const [password, setPassword] = useState("");
  const create = useMutation({
    mutationFn: () => api.setup(setupCode, name, password),
    onSuccess: onCreated,
  });
  const submit = (event: FormEvent) => {
    event.preventDefault();
    create.mutate();
  };
  return (
    <form onSubmit={submit} className="mt-2 grid gap-6">
      <div className="grid gap-3">
        <h1 className="text-[26px] leading-tight font-extrabold tracking-[-0.02em]">
          {t("setup.title")}
        </h1>
        <p className="text-muted-foreground">{t("setup.intro")}</p>
      </div>
      <Field
        label={t("setup.code")}
        hint={t("setup.code_hint")}
        value={setupCode}
        onChange={(event) => onSetupCodeChange(event.target.value)}
        autoComplete="off"
        autoCapitalize="characters"
        spellCheck={false}
        required
      />
      <Field
        label={t("setup.name")}
        value={name}
        onChange={(event) => setName(event.target.value)}
        autoComplete="username"
        maxLength={64}
        required
      />
      <Field
        label={t("setup.password")}
        hint={t("setup.password_hint")}
        type="password"
        value={password}
        onChange={(event) => setPassword(event.target.value)}
        autoComplete="new-password"
        minLength={12}
        maxLength={256}
        required
      />
      {create.error && <ErrorText error={create.error} />}
      <Button type="submit" size="lg" disabled={create.isPending}>
        {t("setup.continue")}
      </Button>
    </form>
  );
}

function AuthenticatorStep({
  setupCode,
  answer,
}: {
  setupCode: string;
  answer: SetupAnswer;
}) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [code, setCode] = useState("");
  const confirm = useMutation({
    mutationFn: () => api.confirmSetup(setupCode, code),
    onSuccess: async () => {
      await queryClient.invalidateQueries();
      navigate("/", { replace: true });
    },
  });
  const submit = (event: FormEvent) => {
    event.preventDefault();
    confirm.mutate();
  };
  return (
    <form onSubmit={submit} className="mt-2 grid gap-6">
      <div className="grid gap-3">
        <h1 className="text-[26px] leading-tight font-extrabold tracking-[-0.02em]">
          {t("setup.totp_title")}
        </h1>
        <p className="text-muted-foreground">{t("setup.totp_intro")}</p>
      </div>
      <img
        src={answer.qr_code}
        alt=""
        className="size-48 rounded-md border border-line bg-white p-2 [image-rendering:pixelated]"
      />
      <div className="grid gap-1.5 text-sm">
        <span className="text-muted-foreground">{t("setup.totp_manual")}</span>
        <code className="figures rounded-sm bg-raised px-2 py-1.5 font-medium break-all select-all">
          {answer.totp_secret.match(/.{1,4}/g)?.join(" ")}
        </code>
      </div>
      <Field
        label={t("setup.totp_code")}
        value={code}
        onChange={(event) => setCode(event.target.value.replace(/\D/g, ""))}
        inputMode="numeric"
        autoComplete="one-time-code"
        pattern="\d{6}"
        maxLength={6}
        className="figures text-lg tracking-[0.3em]"
        required
      />
      {confirm.error && <ErrorText error={confirm.error} />}
      <Button type="submit" size="lg" disabled={confirm.isPending}>
        {t("setup.finish")}
      </Button>
    </form>
  );
}

function ErrorText({ error }: { error: Error }) {
  const { t } = useTranslation();
  const code = error instanceof ApiError ? error.code : undefined;
  return (
    <Notice>
      {t(`setup.errors.${code}`, { defaultValue: t("common.error") })}
    </Notice>
  );
}