package de.lukasbreuer.clarity.api.business.invoice.reading;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

/**
 * Reads what a PDF holds: the e-invoice XML that ZUGFeRD and Factur-X embed,
 * otherwise its text, otherwise (for a scan) the text Tesseract recognises
 * on its first pages. Encrypted PDFs are skipped.
 */
@Component
@RequiredArgsConstructor(staticName = "create")
public final class PdfInvoiceReader {
  private static final int MAXIMUM_TEXT_PAGES = 50;
  private static final int MAXIMUM_OCR_PAGES = 3;
  private static final int MINIMUM_TEXT_LETTERS = 20;
  private static final float OCR_DPI = 300;

  private final Ocr ocr;

  public Optional<PdfContent> read(byte[] pdf) {
    try (var document = Loader.loadPDF(pdf)) {
      return document.isEncrypted() ? Optional.empty() :
        Optional.of(content(document));
    } catch (IOException | RuntimeException exception) {
      return Optional.empty();
    }
  }

  /**
   * A PDF with hardly any text and no e-invoice is taken for a scan.
   */
  private PdfContent content(PDDocument document) throws IOException {
    var xml = EmbeddedInvoiceXml.of(document).find();
    var text = text(document);
    if (xml.isEmpty() && letters(text) < MINIMUM_TEXT_LETTERS &&
      ocr.isAvailable()) {
      return new PdfContent(xml, recognise(document), true);
    }
    return new PdfContent(xml, text, false);
  }

  private String text(PDDocument document) throws IOException {
    var stripper = new PDFTextStripper();
    stripper.setEndPage(MAXIMUM_TEXT_PAGES);
    return stripper.getText(document);
  }

  private long letters(String text) {
    return text.chars().filter(Character::isLetter).count();
  }

  private String recognise(PDDocument document) throws IOException {
    var renderer = new PDFRenderer(document);
    var text = new StringBuilder();
    var pages = Math.min(document.getNumberOfPages(), MAXIMUM_OCR_PAGES);
    for (var page = 0; page < pages; page++) {
      recognisePage(renderer, page).ifPresent(pageText -> text
        .append(pageText).append('\n'));
    }
    return text.toString();
  }

  private Optional<String> recognisePage(PDFRenderer renderer, int page)
    throws IOException {
    var image = renderer.renderImageWithDPI(page, OCR_DPI, ImageType.GRAY);
    var png = new ByteArrayOutputStream();
    ImageIO.write(image, "png", png);
    return ocr.read(png.toByteArray());
  }
}