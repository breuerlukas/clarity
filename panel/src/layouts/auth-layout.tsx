import type { ReactNode } from "react";
import { useTranslation } from "react-i18next";
import { Wordmark } from "@/components/wordmark";

/** Setup and login: one card in the middle of the page, the mark above it. */
export function AuthLayout({ children }: { children: ReactNode }) {
  const { t } = useTranslation();
  return (
    <div className="flex min-h-dvh flex-col items-center px-4 py-12 sm:justify-center">
      <div className="w-full max-w-[420px]">
        <Wordmark className="mb-8" />
        <div className="rounded-3xl border bg-card p-6 sm:p-8">{children}</div>
        <p className="mt-6 text-center text-sm text-muted-foreground">{t("auth.promise")}</p>
      </div>
    </div>
  );
}
