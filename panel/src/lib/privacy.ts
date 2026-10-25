import { useSyncExternalStore } from "react";

const STORAGE_KEY = "clarity-amounts-hidden";
const listeners = new Set<() => void>();

/**
 * Whether amounts are hidden from onlookers. Kept in localStorage so a
 * reload doesn't reveal everything again; visible by default and whenever
 * storage is refused.
 */
export function amountsHidden(): boolean {
  try {
    return localStorage.getItem(STORAGE_KEY) === "true";
  } catch {
    return false;
  }
}

export function setAmountsHidden(hidden: boolean) {
  try {
    localStorage.setItem(STORAGE_KEY, String(hidden));
  } catch {
    // Private windows may refuse storage; amounts can't be hidden there.
  }
  listeners.forEach((listener) => listener());
}

export function useAmountsHidden(): [boolean, (hidden: boolean) => void] {
  const hidden = useSyncExternalStore(
    (listener) => {
      listeners.add(listener);
      return () => listeners.delete(listener);
    },
    amountsHidden,
  );
  return [hidden, setAmountsHidden];
}

/**
 * A formatted amount with its figures and sign replaced by dots, so neither
 * the size nor the direction shows; the currency stays on the side and with
 * the spacing the locale gives it ("•••• €", "€••••").
 */
export function maskAmount(parts: Intl.NumberFormatPart[]): string {
  const currencyAt = parts.findIndex((part) => part.type === "currency");
  const currency = parts[currencyAt]?.value ?? "";
  const gap = parts.some((part) => part.type === "literal" && !part.value.trim()) ? "\u00a0" : "";
  return currencyAt < parts.findIndex((part) => part.type === "integer")
    ? `${currency}${gap}••••`
    : `••••${gap}${currency}`;
}
