import { useState, type FormEvent } from "react";
import { Navigate, useLocation, useNavigate, type Location } from "react-router";
import { useTranslation } from "react-i18next";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { AuthLayout } from "@/layouts/auth-layout";
import { Field } from "@/components/field";
import { Notice } from "@/components/notice";
import { Splash } from "@/components/splash";
import { Button } from "@/components/ui/button";
import { useSession, useSetupStatus } from "@/hooks/use-session";
import { api, ApiError } from "@/lib/api";

export function LoginPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const queryClient = useQueryClient();
  const setup = useSetupStatus();
  const session = useSession();
  const [name, setName] = useState("");
  const [password, setPassword] = useState("");
  const [code, setCode] = useState("");
  const from = (location.state as { from?: Location } | null)?.from;
  const login = useMutation({
    mutationFn: () => api.login(name, password, code),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["session"] });
      navigate(from ? `${from.pathname}${from.search}` : "/", { replace: true });
    },
    onError: () => setCode(""),
  });
  if (setup.isPending || session.isPending) {
    return <Splash />;
  }
  if (setup.data?.setup_required) {
    return <Navigate to="/setup" replace />;
  }
  if (session.data) {
    return <Navigate to="/" replace />;
  }
  const submit = (event: FormEvent) => {
    event.preventDefault();
    login.mutate();
  };
  const errorCode =
    login.error instanceof ApiError ? login.error.code : undefined;
  return (
    <AuthLayout>
      {import.meta.env.VITE_DEMO === "true" && (
        <div className="mb-6 grid gap-3 rounded-2xl bg-brand/10 p-4 text-sm">
          <p>{t("login.demo")}</p>
          <Button type="button" onClick={() => login.mutate()} disabled={login.isPending}>
            {t("login.demo_back")}
          </Button>
        </div>
      )}
      <form onSubmit={submit} className="grid gap-6">
        <h1 className="text-[26px] leading-tight font-extrabold tracking-[-0.02em]">
          {t("login.title")}
        </h1>
        <Field
          label={t("login.name")}
          value={name}
          onChange={(event) => setName(event.target.value)}
          autoComplete="username"
          required
        />
        <Field
          label={t("login.password")}
          type="password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          autoComplete="current-password"
          required
        />
        <Field
          label={t("login.code")}
          value={code}
          onChange={(event) => setCode(event.target.value.replace(/\D/g, ""))}
          inputMode="numeric"
          autoComplete="one-time-code"
          pattern="\d{6}"
          maxLength={6}
          className="figures text-lg tracking-[0.3em]"
          required
        />
        {login.error && (
          <Notice>
            {t(`login.errors.${errorCode}`, { defaultValue: t("common.error") })}
          </Notice>
        )}
        <Button type="submit" size="lg" disabled={login.isPending}>
          {t("login.submit")}
        </Button>
      </form>
    </AuthLayout>
  );
}