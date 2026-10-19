import { useTranslation } from "react-i18next";
import { ReceiptText } from "lucide-react";
import { Panel } from "@/components/panel";
import { useReceipts } from "@/hooks/use-cash";
import type { Receipt } from "@/lib/api";
import { formatDate, formatMoney } from "@/lib/format";

/** Receipts that were scanned but not yet assigned; hidden while there are none. */
export function OpenReceipts({ onOpen }: { onOpen: (receipt: Receipt) => void }) {
  const { t } = useTranslation();
  const receipts = useReceipts();
  const open = receipts.data?.filter((receipt) => receipt.transaction === null) ?? [];
  if (open.length === 0) {
    return null;
  }
  return (
    <Panel title={t("receipts.open_title")}>
      <ul className="grid gap-1">
        {open.map((receipt) => (
          <li key={receipt.id}>
            <button
              type="button"
              onClick={() => onOpen(receipt)}
              className="grid w-full grid-cols-[auto_1fr_auto] items-center gap-3 rounded-xl px-2 py-2 text-left hover:bg-raised"
            >
              <ReceiptText className="size-5 text-muted-foreground" aria-hidden />
              <span className="grid min-w-0">
                <span className="truncate font-bold">{receipt.merchant || t("receipts.unknown_merchant")}</span>
                <span className="text-sm text-muted-foreground">{formatDate(receipt.date ?? receipt.created_at)}</span>
              </span>
              <span className="figures text-sm font-bold">{receipt.total === null ? "–" : formatMoney(receipt.total)}</span>
            </button>
          </li>
        ))}
      </ul>
    </Panel>
  );
}
