import { useSyncExternalStore } from "react";

export type ThemePreference = "system" | "light" | "dark";

const STORAGE_KEY = "clarity-theme";
const darkQuery = () => window.matchMedia("(prefers-color-scheme: dark)");
const listeners = new Set<() => void>();

/**
 * The theme lives in localStorage and as a class on <html>. Applied from
 * main.tsx before the first render, so there is no flash and no inline
 * script for the Content Security Policy to block.
 */
export function readTheme(): ThemePreference {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    return stored === "light" || stored === "dark" ? stored : "system";
  } catch {
    return "system";
  }
}

export function applyTheme(preference = readTheme()) {
  const dark =
    preference === "dark" || (preference === "system" && darkQuery().matches);
  document.documentElement.classList.toggle("dark", dark);
}

export function setTheme(preference: ThemePreference) {
  try {
    localStorage.setItem(STORAGE_KEY, preference);
  } catch {
    // Private windows may refuse storage; the theme still applies for now.
  }
  applyTheme(preference);
  listeners.forEach((listener) => listener());
}

export function watchSystemTheme() {
  darkQuery().addEventListener("change", () => applyTheme());
}

export function useTheme(): [ThemePreference, (preference: ThemePreference) => void] {
  const theme = useSyncExternalStore(
    (listener) => {
      listeners.add(listener);
      return () => listeners.delete(listener);
    },
    readTheme,
  );
  return [theme, setTheme];
}