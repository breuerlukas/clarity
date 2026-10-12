import type { RecurringPayment, Transaction } from "@/lib/api";
import { TopCounterparties } from "@/pages/statistics/top-counterparties";
import { LargestExpenses } from "@/pages/statistics/largest-expenses";

export function CounterpartiesTab({
  transactions,
  payments,
}: {
  transactions: Transaction[];
  payments?: RecurringPayment[];
}) {
  return (
    <div className="grid items-start gap-6 xl:grid-cols-2">
      <TopCounterparties transactions={transactions} />
      <LargestExpenses transactions={transactions} payments={payments} />
    </div>
  );
}
