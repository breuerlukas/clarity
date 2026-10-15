import type { CSSProperties } from "react";
import {
  ArrowLeftRight,
  Baby,
  Banknote,
  BookOpen,
  Briefcase,
  Bus,
  Car,
  CircleDashed,
  Coffee,
  Dumbbell,
  Gamepad2,
  Gift,
  HeartPulse,
  Home,
  Landmark,
  Music,
  PawPrint,
  PiggyBank,
  Plane,
  Plug,
  Receipt,
  Repeat,
  ShieldCheck,
  Shirt,
  ShoppingBag,
  ShoppingCart,
  Smartphone,
  Sprout,
  Store,
  Ticket,
  Utensils,
  Wrench,
  type LucideIcon,
} from "lucide-react";

/** The icons a category can have, by the name the api stores. */
export const CATEGORY_ICONS: { id: string; icon: LucideIcon }[] = [
  { id: "shopping-cart", icon: ShoppingCart },
  { id: "utensils", icon: Utensils },
  { id: "shopping-bag", icon: ShoppingBag },
  { id: "home", icon: Home },
  { id: "plug", icon: Plug },
  { id: "repeat", icon: Repeat },
  { id: "shield-check", icon: ShieldCheck },
  { id: "bus", icon: Bus },
  { id: "plane", icon: Plane },
  { id: "heart-pulse", icon: HeartPulse },
  { id: "ticket", icon: Ticket },
  { id: "book-open", icon: BookOpen },
  { id: "banknote", icon: Banknote },
  { id: "receipt", icon: Receipt },
  { id: "piggy-bank", icon: PiggyBank },
  { id: "briefcase", icon: Briefcase },
  { id: "landmark", icon: Landmark },
  { id: "arrow-left-right", icon: ArrowLeftRight },
  { id: "circle-dashed", icon: CircleDashed },
  { id: "paw-print", icon: PawPrint },
  { id: "baby", icon: Baby },
  { id: "gift", icon: Gift },
  { id: "gamepad", icon: Gamepad2 },
  { id: "dumbbell", icon: Dumbbell },
  { id: "music", icon: Music },
  { id: "car", icon: Car },
  { id: "coffee", icon: Coffee },
  { id: "shirt", icon: Shirt },
  { id: "wrench", icon: Wrench },
  { id: "smartphone", icon: Smartphone },
  { id: "sprout", icon: Sprout },
  { id: "store", icon: Store },
];

export function categoryIcon(id: string): LucideIcon {
  return CATEGORY_ICONS.find((entry) => entry.id === id)?.icon ?? CircleDashed;
}

/**
 * The colors a category can have. Gray keeps the quiet look of the rest of
 * the panel; the others tint the icon (`--category-<color>` in global.css).
 */
export const CATEGORY_COLORS = ["gray", "blue", "teal", "green", "amber", "orange", "red", "pink", "violet"];

export function categoryColorStyle(color: string): CSSProperties | undefined {
  if (color === "gray" || !CATEGORY_COLORS.includes(color)) {
    return undefined;
  }
  const value = `var(--category-${color})`;
  return { color: value, backgroundColor: `color-mix(in oklab, ${value} 14%, var(--card))` };
}