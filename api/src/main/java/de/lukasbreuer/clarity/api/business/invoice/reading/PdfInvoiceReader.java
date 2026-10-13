package de.lukasbreuer.clarity.api.business.invoice.reading;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDNameTreeNode;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Reads what a PDF holds: the e-invoice XML that ZUGFeRD and Factur-X embed,
 * otherwise its text, otherwise (for a scan) the text Tesseract recognises
 * on its first pages. Encrypted PDFs are skipped.
 */
public final class PdfInvoiceReader {
  private static final int MAXIMUM_TEXT_PAGES = 50;
  private static final int MAXIMUM_OCR_PAGES = 3;
  private static final int MINIMUM_TEXT_LETTERS = 20;
  private static final int MAXIMUM_XML_BYTES = 5 * 1024 * 1024;
  private static final float OCR_DPI = 300;

  /**
   * @param xml The embedded e-invoice, if there is one
   * @param recognised Whether the text came from OCR
   */
  public record Content(Optional<byte[]> xml, String text, boolean recognised) {
  }

  private PdfInvoiceReader() {
  }

  public static Optional<Content> read(byte[] pdf, Ocr ocr) {
    try (var document = Loader.loadPDF(pdf)) {
      if (document.isEncrypted()) {
        return Optional.empty();
      }
      var xml = embeddedInvoice(document);
      var text = text(document);
      if (xml.isEmpty() && letters(text) < MINIMUM_TEXT_LETTERS &&
        ocr.isAvailable()) {
        return Optional.of(new Content(xml, recognise(document, ocr), true));
      }
      return Optional.of(new Content(xml, text, false));
    } catch (IOException | RuntimeException exception) {
      return Optional.empty();
    }
  }

  private static String text(PDDocument document) throws IOException {
    var stripper = new PDFTextStripper();
    stripper.setEndPage(MAXIMUM_TEXT_PAGES);
    return stripper.getText(document);
  }

  private static long letters(String text) {
    return text.chars().filter(Character::isLetter).count();
  }

  private static String recognise(PDDocument document, Ocr ocr)
    throws IOException {
    var renderer = new PDFRenderer(document);
    var text = new StringBuilder();
    var pages = Math.min(document.getNumberOfPages(), MAXIMUM_OCR_PAGES);
    for (var page = 0; page < pages; page++) {
      var image = renderer.renderImageWithDPI(page, OCR_DPI, ImageType.GRAY);
      var png = new ByteArrayOutputStream();
      ImageIO.write(image, "png", png);
      ocr.read(png.toByteArray()).ifPresent(pageText -> text.append(pageText)
        .append('\n'));
    }
    return text.toString();
  }

  /**
   * ZUGFeRD names the file zugferd-invoice.xml (1.0) or zugferd.xml,
   * Factur-X and ZUGFeRD 2 factur-x.xml, XRechnung inside a PDF
   * xrechnung.xml; any other embedded XML is tried last.
   */
  private static Optional<byte[]> embeddedInvoice(PDDocument document)
    throws IOException {
    var names = document.getDocumentCatalog().getNames();
    if (names == null || names.getEmbeddedFiles() == null) {
      return Optional.empty();
    }
    var files = new ArrayList<EmbeddedXml>();
    collect(names.getEmbeddedFiles(), files);
    return files.stream()
      .sorted((first, second) -> Boolean.compare(!first.isKnownName(),
        !second.isKnownName()))
      .map(EmbeddedXml::content)
      .findFirst();
  }

  private record EmbeddedXml(String name, byte[] content) {
    boolean isKnownName() {
      return List.of("factur-x.xml", "zugferd-invoice.xml", "zugferd.xml",
        "xrechnung.xml").contains(name);
    }
  }

  private static void collect(
    PDNameTreeNode<PDComplexFileSpecification> node, List<EmbeddedXml> files
  ) throws IOException {
    if (node.getNames() != null) {
      for (var entry : node.getNames().entrySet()) {
        var name = entry.getKey().toLowerCase(Locale.ROOT);
        var file = embeddedFile(entry.getValue());
        if (name.endsWith(".xml") && file != null &&
          file.getLength() <= MAXIMUM_XML_BYTES) {
          files.add(new EmbeddedXml(name, file.toByteArray()));
        }
      }
    }
    if (node.getKids() != null) {
      for (var kid : node.getKids()) {
        collect(kid, files);
      }
    }
  }

  private static PDEmbeddedFile embeddedFile(
    PDComplexFileSpecification specification
  ) {
    var file = specification.getEmbeddedFileUnicode();
    return file != null ? file : specification.getEmbeddedFile();
  }
}
