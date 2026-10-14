import { useLayoutEffect, useRef } from "react";

/**
 * A highlight that slides to the selected choice instead of jumping. It is
 * moved directly on the element rather than through state, and only
 * animates after its first placement so it doesn't fly in on page load.
 */
export function useSlidingPill<T extends HTMLElement>(selector: string) {
  const container = useRef<T>(null);
  const pill = useRef<HTMLSpanElement>(null);
  useLayoutEffect(() => {
    const element = container.current;
    if (!element) {
      return;
    }
    const update = () => {
      const active = element.querySelector<HTMLElement>(selector);
      const style = pill.current?.style;
      if (!style) {
        return;
      }
      style.opacity = active ? "1" : "0";
      if (active) {
        style.width = `${active.offsetWidth}px`;
        style.height = `${active.offsetHeight}px`;
        style.transform = `translate(${active.offsetLeft}px, ${active.offsetTop}px)`;
      }
    };
    update();
    const frame = requestAnimationFrame(() => pill.current?.setAttribute("data-ready", ""));
    const mutations = new MutationObserver(update);
    mutations.observe(element, {
      attributes: true,
      attributeFilter: ["data-state", "aria-checked"],
      subtree: true,
    });
    const resizes = new ResizeObserver(update);
    resizes.observe(element);
    return () => {
      cancelAnimationFrame(frame);
      mutations.disconnect();
      resizes.disconnect();
    };
  }, [selector]);
  return { container, pill };
}

/** Classes for the pill itself; its look comes from the caller. */
export const PILL_CLASS =
  "pointer-events-none absolute top-0 left-0 rounded-full opacity-0 data-[ready]:transition-[transform,width,height] data-[ready]:duration-300 data-[ready]:ease-out";
