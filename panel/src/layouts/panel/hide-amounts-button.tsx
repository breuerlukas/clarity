import { useEffect } from "react";
import { useTranslation } from "react-i18next";
import { Eye, EyeOff } from "lucide-react";
import { useAmountsHidden } from "@/lib/privacy";

/** Typing "H" into a field must not hide the amounts. */
function isTyping(target: EventTarget | null) {
  return target instanceof HTMLElement && (target.isContentEditable || ["INPUT", "TEXTAREA", "SELECT"].includes(target.tagName));
}

/**
 * Hides or shows every amount at once, for a glance from the side; also on
 * Shift+H.
 */
export function HideAmountsButton() {
  const { t } = useTranslation();
  const [hidden, setHidden] = useAmountsHidden();
  useEffect(() => {
    const toggle = (event: KeyboardEvent) => {
      if (event.code === "KeyH" && event.shiftKey && !event.ctrlKey && !event.metaKey && !event.altKey && !isTyping(event.target)) {
        setHidden(!hidden);
      }
    };
    window.addEventListener("keydown", toggle);
    return () => window.removeEventListener("keydown", toggle);
  }, [hidden, setHidden]);
  const label = t(hidden ? "nav.show_amounts" : "nav.hide_amounts");
  return (
    <button
      type="button"
      onClick={() => setHidden(!hidden)}
      aria-label={label}
      aria-pressed={hidden}
      title={`${label} (Shift+H)`}
      className="grid size-9 shrink-0 place-items-center rounded-[10px] text-muted-foreground transition-colors hover:bg-panel hover:text-foreground"
    >
      {hidden ? <EyeOff size={18} aria-hidden /> : <Eye size={18} aria-hidden />}
    </button>
  );
}
