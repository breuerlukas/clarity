package de.lukasbreuer.clarity.api.business.invoice.reading;

import de.lukasbreuer.clarity.api.business.invoice.InvoiceFormat;
import de.lukasbreuer.clarity.api.log.Log;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

final class PdfInvoiceReaderTest {
  private static final LocalDate RECEIVED = LocalDate.of(2026, 10, 5);

  @Test
  void readsEmbeddedInvoice() throws IOException {
    var pdf = pdf(List.of("Rechnung RE-2026-0042"),
      ElectronicInvoiceReaderTest.file("xrechnung-cii.xml"));
    var result = recognition().recognise(new InvoiceFile("rechnung.pdf",
      "application/pdf", pdf), RECEIVED).invoice().orElseThrow();
    assertEquals(InvoiceFormat.ZUGFERD, result.format());
    assertFalse(result.needsReview());
    assertEquals("Nordlicht Hosting GmbH", result.fields().issuer());
    assertEquals(new BigDecimal("119.00"), result.fields().grossAmount());
  }

  @Test
  void readsTextOfPlainPdf() throws IOException {
    var pdf = pdf(List.of("Rechnung", "Rechnungsnummer: 4711",
      "Rechnungsdatum: 02.10.2026", "Gesamtbetrag 59,50 EUR"), null);
    var result = recognition().recognise(new InvoiceFile("4711.pdf",
      "application/octet-stream", pdf), RECEIVED).invoice().orElseThrow();
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
    var recognition = recognition();
    var image = new BufferedImage(1240, 700, BufferedImage.TYPE_INT_RGB);
    var graphics = image.createGraphics();
    graphics.setColor(Color.WHITE);
    graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
    graphics.setColor(Color.BLACK);
    graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
      RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 36));
    var lines = List.of("INVOICE", "Invoice No. 4711", "Date: 2026-10-02",
      "Total amount 59.50 EUR");
    for (var index = 0; index < lines.size(); index++) {
      graphics.drawString(lines.get(index), 80, 120 + index * 80);
    }
    graphics.dispose();
    var png = new ByteArrayOutputStream();
    ImageIO.write(image, "png", png);
    var file = new InvoiceFile("scan.png", "image/png", png.toByteArray());
    var result = recognition.recognise(file, RECEIVED);
    assumeTrue(!result.text().isEmpty(), "Tesseract is not installed");
    var invoice = result.invoice().orElseThrow();
    assertEquals(InvoiceFormat.OCR, invoice.format());
    assertTrue(invoice.needsReview());
    assertEquals("4711", invoice.fields().number());
    assertEquals(new BigDecimal("59.50"), invoice.fields().grossAmount());
  }

  @Test
  void skipsBrokenPdf() {
    assertTrue(recognition().recognise(new InvoiceFile("broken.pdf",
      "application/pdf", "%PDF-1.7 nonsense".getBytes()), RECEIVED)
      .invoice().isEmpty());
  }

  private InvoiceRecognition recognition() {
    try {
      return new InvoiceRecognition(new Ocr(Log.create("Test")));
    } catch (Exception exception) {
      throw new IllegalStateException(exception);
    }
  }

  private byte[] pdf(List<String> lines, byte[] xml) throws IOException {
    try (var document = new PDDocument()) {
      var page = new PDPage();
      document.addPage(page);
      try (var content = new PDPageContentStream(document, page)) {
        content.beginText();
        content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA),
          12);
        content.newLineAtOffset(50, 700);
        for (var line : lines) {
          content.showText(line);
          content.newLineAtOffset(0, -16);
        }
        content.endText();
      }
      if (xml != null) {
        embed(document, xml);
      }
      var output = new ByteArrayOutputStream();
      document.save(output);
      return output.toByteArray();
    }
  }

  private void embed(PDDocument document, byte[] xml) throws IOException {
    var file = new PDEmbeddedFile(document, new ByteArrayInputStream(xml));
    file.setSubtype("text/xml");
    var specification = new PDComplexFileSpecification();
    specification.setFile("factur-x.xml");
    specification.setFileUnicode("factur-x.xml");
    specification.setEmbeddedFile(file);
    specification.setEmbeddedFileUnicode(file);
    var tree = new PDEmbeddedFilesNameTreeNode();
    tree.setNames(Map.of("factur-x.xml", specification));
    var names = new PDDocumentNameDictionary(document.getDocumentCatalog());
    names.setEmbeddedFiles(tree);
    document.getDocumentCatalog().setNames(names);
  }
}
