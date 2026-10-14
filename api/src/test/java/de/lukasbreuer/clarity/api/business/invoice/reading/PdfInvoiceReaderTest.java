package de.lukasbreuer.clarity.api.business.invoice.reading;

import de.lukasbreuer.clarity.api.business.invoice.InvoiceFormat;
import de.lukasbreuer.clarity.api.log.Log;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

final class PdfInvoiceReaderTest {
  private static final LocalDate RECEIVED = LocalDate.of(2026, 10, 5);

  private final InvoiceRecognition recognition = recognition();

  @Test
  void readsEmbeddedInvoice() throws IOException {
    var pdf = InvoicePdf.of(List.of("Rechnung RE-2026-0042"),
      InvoiceResource.of("xrechnung-cii.xml").bytes()).bytes();
    var result = recognise(new InvoiceFile("rechnung.pdf",
      "application/pdf", pdf)).invoice().orElseThrow();
    assertEquals(InvoiceFormat.ZUGFERD, result.format());
    assertFalse(result.needsReview());
    assertEquals("Nordlicht Hosting GmbH", result.fields().issuer());
    assertEquals(new BigDecimal("119.00"), result.fields().grossAmount());
  }

  @Test
  void readsTextOfPlainPdf() throws IOException {
    var pdf = InvoicePdf.of(List.of("Rechnung", "Rechnungsnummer: 4711",
      "Rechnungsdatum: 02.10.2026", "Gesamtbetrag 59,50 EUR"), null).bytes();
    var result = recognise(new InvoiceFile("4711.pdf",
      "application/octet-stream", pdf)).invoice().orElseThrow();
    assertEquals(InvoiceFormat.PDF, result.format());
    assertTrue(result.needsReview());
    assertEquals("4711", result.fields().number());
    assertEquals(new BigDecimal("59.50"), result.fields().grossAmount());
  }

  /**
   * Only where Tesseract is installed, as in the api image.
   */
  @Test
  void recognisesScannedInvoice() throws IOException {
    var png = ScannedInvoice.of(List.of("INVOICE", "Invoice No. 4711",
      "Date: 2026-10-02", "Total amount 59.50 EUR")).png();
    var result = recognise(new InvoiceFile("scan.png", "image/png", png));
    assumeTrue(!result.text().isEmpty(), "Tesseract is not installed");
    var invoice = result.invoice().orElseThrow();
    assertEquals(InvoiceFormat.OCR, invoice.format());
    assertTrue(invoice.needsReview());
    assertEquals("4711", invoice.fields().number());
    assertEquals(new BigDecimal("59.50"), invoice.fields().grossAmount());
  }

  @Test
  void skipsBrokenPdf() {
    assertTrue(recognise(new InvoiceFile("broken.pdf", "application/pdf",
      "%PDF-1.7 nonsense".getBytes())).invoice().isEmpty());
  }

  private InvoiceRecognition.Result recognise(InvoiceFile file) {
    return recognition.recognise(file, RECEIVED);
  }

  private InvoiceRecognition recognition() {
    var ocr = Ocr.create(testLog());
    return InvoiceRecognition.create(ocr,
      ElectronicInvoiceReader.create(SecureXml.create()),
      PdfInvoiceReader.create(ocr), TextInvoiceReader.create(
        InvoiceVocabularyFile.load().vocabulary()));
  }

  private Log testLog() {
    try {
      return Log.create("Test");
    } catch (Exception exception) {
      throw new IllegalStateException(exception);
    }
  }
}