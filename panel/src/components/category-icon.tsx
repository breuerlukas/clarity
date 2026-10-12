import {
  ArrowLeftRight,
  Banknote,
  BookOpen,
  Briefcase,
  Bus,
  CircleDashed,
  HeartPulse,
  Home,
  Landmark,
  PiggyBank,
  Plane,
  Plug,
  Receipt,
  Repeat,
  ShieldCheck,
  ShoppingBag,
  ShoppingCart,
  Ticket,
  Utensils,
  type LucideIcon,
} from "lucide-react";
import type { Category } from "@/lib/api";
import { cn } from "@/lib/utils";

const ICONS: Record<Category, LucideIcon> = {
  SALARY: Briefcase,
  INCOME: Landmark,
  HOUSING: Home,
  UTILITIES: Plug,
  GROCERIES: ShoppingCart,
  DINING: Utensils,
  SHOPPING: ShoppingBag,
  MOBILITY: Bus,
  TRAVEL: Plane,
  SUBSCRIPTIONS: Repeat,
  INSURANCE: ShieldCheck,
  HEALTH: HeartPulse,
  LEISURE: Ticket,
  EDUCATION: BookOpen,
  CASH: Banknote,
  FEES: Receipt,
  SAVINGS: PiggyBank,
  TRANSFER: ArrowLeftRight,
  OTHER: CircleDashed,
};

/** The category label always sits next to it; the icon only helps scanning. */
export function CategoryIcon({
  category,
  className,
}: {
  category: Category;
  className?: string;
}) {
  const Icon = ICONS[category];
  return (
    <span
      className={cn(
        "grid size-9 shrink-0 place-items-center rounded-full bg-card text-muted-foreground ring-1 ring-line",
        className,
      )}
      aria-hidden="true"
    >
      <Icon className="size-4" />
    </span>
  );
}