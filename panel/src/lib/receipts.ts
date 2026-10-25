import type { Category, ReceiptItem, SplitPart } from "@/lib/api";

const cents = (amount: number) => Math.round(amount * 100);

export interface CategorizedItem extends ReceiptItem {
  category: Category;
}

/**
 * A payment split along its receipt: the items summed per category, and
 * what they leave of the payment (discounts, deposits, rounding, a line
 * that was not read) in the main category. The parts carry the payment's
 * sign and add up to it exactly; a part with the other sign means the
 * items come to more than was paid.
 */
export function splitFromItems(
  amount: number,
  mainCategory: Category,
  items: CategorizedItem[],
): SplitPart[] {
  const sign = amount < 0 ? -1 : 1;
  const byCategory = new Map<Category, number>([[mainCategory, Math.abs(cents(amount))]]);
  for (const item of items) {
    byCategory.set(item.category, (byCategory.get(item.category) ?? 0) + cents(item.amount));
    byCategory.set(mainCategory, byCategory.get(mainCategory)! - cents(item.amount));
  }
  return [...byCategory.entries()]
    .filter(([, value]) => value !== 0)
    .map(([category, value]) => ({ category, amount: (sign * value) / 100 }));
}

/** Two parts or more, each going the way the payment went. */
export function isSplit(parts: SplitPart[], amount: number) {
  return parts.length > 1 && parts.every((part) => Math.sign(part.amount) === Math.sign(amount));
}