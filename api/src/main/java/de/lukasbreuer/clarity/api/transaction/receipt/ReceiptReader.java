package de.lukasbreuer.clarity.api.transaction.receipt;

import de.lukasbreuer.clarity.api.business.invoice.reading.InvoiceFile;
import de.lukasbreuer.clarity.api.business.invoice.reading.Ocr;
import de.lukasbreuer.clarity.api.business.invoice.reading.PdfContent;
import de.lukasbreuer.clarity.api.business.invoice.reading.PdfInvoiceReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Reads a receipt with the same Tesseract the invoices use: a photo
 * directly, a PDF from its text, or from its pages when it is a scan.
 * Without Tesseract a photo yields nothing and the owner types it in.
 */
@Component
@RequiredArgsConstructor
public final class ReceiptReader {
  private final Ocr ocr;
  private final PdfInvoiceReader pdfReader;
  private final ReceiptVocabulary vocabulary;

  public RecognisedReceipt read(InvoiceFile file) {
    return text(file)
      .map(text -> ReceiptText.of(vocabulary, text).recognise())
      .orElseGet(RecognisedReceipt::nothing);
  }

  private Optional<String> text(InvoiceFile file) {
    if (file.isPdf()) {
      return pdfReader.read(file.content()).map(PdfContent::text);
    }
    return ocr.read(file.content());
  }
}