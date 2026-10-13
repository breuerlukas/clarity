import {
  BriefcaseBusiness,
  Building2,
  Camera,
  Car,
  GraduationCap,
  Hammer,
  Heart,
  House,
  Laptop,
  Palette,
  PiggyBank,
  Plane,
  Sprout,
  Store,
  Users,
  Wallet,
  type LucideIcon,
} from "lucide-react";

/** The icons a space can have, by the name the api stores. */
export const SPACE_ICONS: { id: string; icon: LucideIcon }[] = [
  { id: "wallet", icon: Wallet },
  { id: "house", icon: House },
  { id: "users", icon: Users },
  { id: "heart", icon: Heart },
  { id: "piggy-bank", icon: PiggyBank },
  { id: "plane", icon: Plane },
  { id: "car", icon: Car },
  { id: "graduation-cap", icon: GraduationCap },
  { id: "briefcase", icon: BriefcaseBusiness },
  { id: "building", icon: Building2 },
  { id: "store", icon: Store },
  { id: "palette", icon: Palette },
  { id: "laptop", icon: Laptop },
  { id: "camera", icon: Camera },
  { id: "hammer", icon: Hammer },
  { id: "sprout", icon: Sprout },
];

export function spaceIcon(id: string): LucideIcon {
  return SPACE_ICONS.find((entry) => entry.id === id)?.icon ?? Wallet;
}
