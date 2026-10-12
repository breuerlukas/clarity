import { useTranslation } from "react-i18next";
import { Wordmark } from "@/components/wordmark";

export function Splash() {
  const { t } = useTranslation();
  return (
    <div className="grid min-h-dvh place-items-center" role="status">
      <Wordmark className="opacity-60" />
      <span className="sr-only">{t("common.loading")}</span>
    </div>
  );
}