import { useEffect, useRef } from "react";
import { Link, useNavigate, useSearchParams } from "react-router";
import { useTranslation } from "react-i18next";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { AuthLayout } from "@/layouts/auth-layout";
import { Notice } from "@/components/notice";
import { api, ApiError } from "@/lib/api";

/**
 * Where the bank sends the browser back to. Hands code and state to the api
 * once (StrictMode would otherwise send the one-time code twice), then
 * returns to the personal or business accounts it was started from.
 */
export function ConnectCallbackPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [params] = useSearchParams();
  const sent = useRef(false);
  const code = params.get("code");
  const state = params.get("state");
  const complete = useMutation({
    mutationFn: () => api.completeConnection(code!, state!),
    onSuccess: async ({ business }) => {
      await queryClient.invalidateQueries();
      navigate(business ? "/business/accounts" : "/accounts", { replace: true });
    },
  });
  useEffect(() => {
    if (code && state && !sent.current) {
      sent.current = true;
      complete.mutate();
    }
  }, [code, state, complete]);
  const message = !code
    ? t("connect.cancelled")
    : complete.error instanceof ApiError && complete.error.code === 2001
      ? t("connect.expired")
      : complete.isError
        ? t("connect.failed")
        : null;
  return (
    <AuthLayout>
      {message ? (
        <div className="grid gap-6">
          <Notice>{message}</Notice>
          <Link
            to="/accounts"
            className="font-medium underline decoration-line underline-offset-4"
          >
            {t("connect.back")}
          </Link>
        </div>
      ) : (
        <p role="status" className="text-[17px]">
          {t("connect.working")}
        </p>
      )}
    </AuthLayout>
  );
}