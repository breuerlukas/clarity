import i18n from "@/locales/i18n";

const locale = () => i18n.resolvedLanguage ?? "de";

export function formatMoney(
  amount: number,
  currency = "EUR",
  options: { signed?: boolean; compact?: boolean } = {},
): string {
  return new Intl.NumberFormat(locale(), {
    style: "currency",
    currency,
    signDisplay: options.signed ? "exceptZero" : "auto",
    ...(options.compact
      ? { notation: "compact", maximumFractionDigits: 1 }
      : {}),
  }).format(amount);
}

export function formatPercent(share: number): string {
  return new Intl.NumberFormat(locale(), {
    style: "percent",
    maximumFractionDigits: 0,
  }).format(share);
}

/** "Okt." for chart axes. */
export function formatMonthShort(month: string): string {
  const [year, index] = month.split("-").map(Number);
  return new Intl.DateTimeFormat(locale(), { month: "short" }).format(
    new Date(year, index - 1, 1),
  );
}

export function formatMonth(month: string): string {
  const [year, index] = month.split("-").map(Number);
  return new Intl.DateTimeFormat(locale(), {
    month: "long",
    year: "numeric",
  }).format(new Date(year, index - 1, 1));
}

export function formatDay(date: string): string {
  return new Intl.DateTimeFormat(locale(), {
    weekday: "short",
    day: "numeric",
    month: "short",
  }).format(new Date(`${date}T12:00:00`));
}

export function formatDate(value: string | number): string {
  const date =
    typeof value === "number" ? new Date(value) : new Date(`${value}T12:00:00`);
  return new Intl.DateTimeFormat(locale(), { dateStyle: "medium" }).format(date);
}

export function formatRelative(timestamp: number): string {
  const minutes = Math.round((timestamp - Date.now()) / 60000);
  const format = new Intl.RelativeTimeFormat(locale(), { numeric: "auto" });
  if (Math.abs(minutes) < 60) {
    return format.format(minutes, "minute");
  }
  if (Math.abs(minutes) < 60 * 48) {
    return format.format(Math.round(minutes / 60), "hour");
  }
  return format.format(Math.round(minutes / 1440), "day");
}

/** "DE89 3704 0044 0532 0130 00" becomes "DE89 •••• 3000". */
export function maskIban(iban: string | null): string {
  if (!iban) {
    return "";
  }
  const compact = iban.replace(/\s/g, "");
  return `${compact.slice(0, 4)} •••• ${compact.slice(-4)}`;
}