import { useTranslation } from "react-i18next";
import { Download } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useCategoryLook } from "@/hooks/use-categories";
import { useCashWallet } from "@/hooks/use-cash";
import { useConnections } from "@/hooks/use-finances";
import type { Category, Transaction } from "@/lib/api";
import { transactionsCsv } from "@/lib/csv";

const COLUMNS = ["date", "account", "counterparty", "purpose", "amount", "currency", "category"];

/**
 * Downloads what the list shows as CSV. Built in the browser from the loaded
 * period, so it works in the demo and sends nothing anywhere.
 */
export function ExportButton({
  transactions,
  category,
  fileName,
}: {
  transactions: Transaction[];
  category: Category | null;
  fileName: string;
}) {
  const { t, i18n } = useTranslation();
  const connections = useConnections();
  const wallet = useCashWallet();
  const look = useCategoryLook();
  const accountName = (id: string) =>
    id === wallet.data?.id
      ? t("cash.title")
      : (connections.data?.flatMap((connection) => connection.accounts).find((account) => account.id === id)?.name ?? "");
  const download = () => {
    const csv = transactionsCsv(transactions, {
      language: i18n.resolvedLanguage ?? "de",
      headers: COLUMNS.map((column) => t(`transactions.export_${column}`)),
      accountName,
      categoryName: (key) => look(key).label,
      category,
    });
    const link = document.createElement("a");
    link.href = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" }));
    link.download = fileName;
    link.click();
    URL.revokeObjectURL(link.href);
  };
  return (
    <Button variant="outline" onClick={download} disabled={transactions.length === 0}>
      <Download />
      {t("transactions.export")}
    </Button>
  );
}
