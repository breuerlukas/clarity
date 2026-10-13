import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from "@/components/ui/dialog";
import { Notice } from "@/components/notice";
import { useSpace } from "@/hooks/use-space";
import { api, ApiError, type Bank } from "@/lib/api";
import { cn } from "@/lib/utils";

/** The EU/EEA countries Enable Banking covers. */
const COUNTRIES = ["AT", "BE", "BG", "CY", "CZ", "DE", "DK", "EE", "ES", "FI",
  "FR", "GR", "HR", "HU", "IE", "IS", "IT", "LI", "LT", "LU", "LV", "MT", "NL",
  "NO", "PL", "PT", "RO", "SE", "SI", "SK"];

/**
 * Picks a bank and sends the owner there to sign in. The accounts go to the
 * open space; for a business space the owner signs in as a company.
 */
export function ConnectBankDialog({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const { t, i18n } = useTranslation();
  const space = useSpace();
  const business = space.kind === "BUSINESS";
  const [country, setCountry] = useState("DE");
  const [query, setQuery] = useState("");
  const [bank, setBank] = useState<Bank | null>(null);
  const countryNames = useMemo(
    () => new Intl.DisplayNames([i18n.resolvedLanguage ?? "de"], { type: "region" }),
    [i18n.resolvedLanguage],
  );
  const banks = useQuery({
    queryKey: ["banks", country],
    queryFn: () => api.banks(country),
    select: (data) => data.banks,
    enabled: open,
    staleTime: 60 * 60_000,
  });
  const start = useMutation({
    mutationFn: (selected: Bank) => api.startConnection(selected, space.id),
    onSuccess: ({ url }) => window.location.assign(url),
  });
  const needle = query.trim().toLocaleLowerCase();
  const matches = (banks.data ?? []).filter((candidate) =>
    candidate.name.toLocaleLowerCase().includes(needle),
  );
  const notConfigured =
    banks.error instanceof ApiError && banks.error.code === 2000;
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent closeLabel={t("common.cancel")} className="sm:max-w-xl">
        <div className="grid gap-2">
          <DialogTitle>{t(business ? "connect.title_business" : "connect.title")}</DialogTitle>
          <DialogDescription>{t(business ? "connect.hint_business" : "connect.hint")}</DialogDescription>
        </div>
        <div className="grid gap-3 sm:grid-cols-[12rem_1fr]">
          <div className="grid gap-2">
            <Label htmlFor="connect-country">{t("connect.country")}</Label>
            <select
              id="connect-country"
              value={country}
              onChange={(event) => {
                setCountry(event.target.value);
                setBank(null);
              }}
              className="h-12 rounded-2xl border-[1.5px] border-transparent bg-ground px-4 text-sm text-foreground outline-none focus-visible:border-primary"
            >
              {COUNTRIES.map((code) => (
                <option key={code} value={code}>
                  {countryNames.of(code)}
                </option>
              ))}
            </select>
          </div>
          <div className="grid gap-2">
            <Label htmlFor="connect-search">{t("connect.search")}</Label>
            <div className="relative">
              <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                id="connect-search"
                type="search"
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                className="pl-9"
              />
            </div>
          </div>
        </div>
        {notConfigured && <Notice>{t("accounts.not_configured")}</Notice>}
        {banks.isError && !notConfigured && <Notice>{t("common.error")}</Notice>}
        <ul
          className={cn(
            "zebra -mx-1 h-72 overflow-y-auto rounded-md border border-line p-1",
            banks.isError && "hidden",
          )}
          aria-busy={banks.isPending}
        >
          {banks.data && matches.length === 0 && (
            <li className="p-3 text-muted-foreground">{t("connect.empty")}</li>
          )}
          {matches.map((candidate) => (
            <li key={candidate.name} className="zebra-row rounded-sm">
              <button
                type="button"
                aria-pressed={bank?.name === candidate.name}
                onClick={() => setBank(candidate)}
                className={cn(
                  "w-full rounded-sm px-3 py-2.5 text-left hover:bg-raised",
                  bank?.name === candidate.name && "bg-primary text-primary-foreground hover:bg-primary",
                )}
              >
                {candidate.name}
              </button>
            </li>
          ))}
        </ul>
        {start.isError && (
          <Notice>
            {start.error instanceof ApiError && start.error.code === 9000
              ? t("common.demo_only")
              : t("common.error")}
          </Notice>
        )}
        <Button
          size="lg"
          disabled={!bank || start.isPending}
          onClick={() => bank && start.mutate(bank)}
        >
          {t("connect.continue")}
        </Button>
      </DialogContent>
    </Dialog>
  );
}