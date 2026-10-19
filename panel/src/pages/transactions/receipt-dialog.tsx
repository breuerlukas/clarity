import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useMutation } from "@tanstack/react-query";
import { Camera } from "lucide-react";
import { Dialog, DialogContent, DialogDescription, DialogTitle } from "@/components/ui/dialog";
import { Notice } from "@/components/notice";
import { useRefreshCash } from "@/hooks/use-cash";
import { useSpace } from "@/hooks/use-space";
import { ReceiptAssign } from "@/pages/transactions/receipt-assign";
import { ReceiptReview } from "@/pages/transactions/receipt-review";
import { api, ApiError, type Receipt, type ReceiptUpload } from "@/lib/api";

const MAXIMUM_EDGE = 2000;

/**
 * Scans a receipt (the phone offers its camera for the file input), shows
 * what was read for review, then assigns it to a card payment or books it
 * as cash. An unassigned receipt opens at the review.
 */
export function ReceiptDialog({
  open,
  receipt,
  onOpenChange,
}: {
  open: boolean;
  receipt: Receipt | null;
  onOpenChange: (open: boolean) => void;
}) {
  const { t } = useTranslation();
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent closeLabel={t("common.close")} className="sm:max-w-2xl">
        {open && <ReceiptFlow key={receipt?.id ?? "new"} initial={receipt} onDone={() => onOpenChange(false)} />}
      </DialogContent>
    </Dialog>
  );
}

function ReceiptFlow({ initial, onDone }: { initial: Receipt | null; onDone: () => void }) {
  const refresh = useRefreshCash();
  const [receipt, setReceipt] = useState(initial);
  const [preview, setPreview] = useState<string | null>(null);
  const [step, setStep] = useState<"review" | "assign">("review");
  const finish = async () => {
    await refresh();
    onDone();
  };
  if (!receipt) {
    return (
      <ReceiptPicker
        onScanned={async (scanned, image) => {
          setPreview(image);
          setReceipt(scanned);
          await refresh();
        }}
      />
    );
  }
  return step === "review" ? (
    <ReceiptReview
      receipt={receipt}
      preview={preview}
      onSaved={(saved) => {
        setReceipt(saved);
        setStep("assign");
      }}
      onDiscarded={finish}
    />
  ) : (
    <ReceiptAssign receipt={receipt} onBack={() => setStep("review")} onAssigned={finish} />
  );
}

function ReceiptPicker({ onScanned }: { onScanned: (receipt: Receipt, preview: string | null) => void }) {
  const { t } = useTranslation();
  const space = useSpace();
  const scan = useMutation({
    mutationFn: async (file: File) => {
      const { upload, preview } = await prepare(file);
      return { receipt: (await api.scanReceipt(space.id, upload)).receipt, preview };
    },
    onSuccess: ({ receipt, preview }) => onScanned(receipt, preview),
  });
  return (
    <>
      <div className="grid gap-2">
        <DialogTitle>{t("receipts.scan")}</DialogTitle>
        <DialogDescription>{t("receipts.scan_hint")}</DialogDescription>
      </div>
      <label className="grid cursor-pointer place-items-center gap-3 rounded-3xl border-2 border-dashed border-divider p-10 text-center hover:bg-raised has-[:focus-visible]:border-primary">
        <Camera className="size-8 text-muted-foreground" aria-hidden />
        <span className="font-bold">{t(scan.isPending ? "receipts.reading" : "receipts.choose")}</span>
        <span className="text-sm text-muted-foreground">{t("receipts.choose_hint")}</span>
        <input
          type="file"
          accept="image/*,application/pdf"
          className="sr-only"
          disabled={scan.isPending}
          onChange={(event) => event.target.files?.[0] && scan.mutate(event.target.files[0])}
        />
      </label>
      {scan.isError && (
        <Notice>
          {scan.error instanceof ApiError && scan.error.code && t(`receipts.errors.${scan.error.code}`, { defaultValue: "" })
            ? t(`receipts.errors.${scan.error.code}`)
            : t("common.error")}
        </Notice>
      )}
    </>
  );
}

/**
 * Photos shrink to 2000 pixels on the long edge as JPEG: plenty for the
 * text recognition, and a phone photo of several megabytes uploads fast.
 * The preview is a data address, the only kind the panel loads besides its
 * own server.
 */
async function prepare(file: File): Promise<{ upload: ReceiptUpload; preview: string | null }> {
  if (file.type === "application/pdf" || file.name.toLowerCase().endsWith(".pdf")) {
    const content = await new Promise<string>((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(String(reader.result).split(",")[1]);
      reader.onerror = () => reject(reader.error);
      reader.readAsDataURL(file);
    });
    return { upload: { name: file.name, content_type: "application/pdf", content }, preview: null };
  }
  const bitmap = await createImageBitmap(file);
  const scale = Math.min(1, MAXIMUM_EDGE / Math.max(bitmap.width, bitmap.height));
  const canvas = document.createElement("canvas");
  canvas.width = Math.round(bitmap.width * scale);
  canvas.height = Math.round(bitmap.height * scale);
  canvas.getContext("2d")!.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
  const preview = canvas.toDataURL("image/jpeg", 0.85);
  const name = `${file.name.replace(/\.[^.]*$/, "") || "receipt"}.jpg`;
  return { upload: { name, content_type: "image/jpeg", content: preview.split(",")[1] }, preview };
}
