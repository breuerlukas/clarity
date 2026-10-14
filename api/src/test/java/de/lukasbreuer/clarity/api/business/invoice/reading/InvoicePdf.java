package de.lukasbreuer.clarity.api.business.invoice.reading;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * A one-page PDF with lines of text and, for ZUGFeRD, an embedded
 * factur-x.xml.
 */
@RequiredArgsConstructor(staticName = "of")
final class InvoicePdf {
  private final List<String> lines;
  private final byte[] xml;

  byte[] bytes() throws IOException {
    try (var document = new PDDocument()) {
      var page = new PDPage();
      document.addPage(page);
      write(document, page);
      if (xml != null) {
        embed(document);
      }
      var output = new ByteArrayOutputStream();
      document.save(output);
      return output.toByteArray();
    }
  }

  private void write(PDDocument document, PDPage page) throws IOException {
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
  }

  private void embed(PDDocument document) throws IOException {
    var tree = new PDEmbeddedFilesNameTreeNode();
    tree.setNames(Map.of("factur-x.xml", specification(document)));
    var names = new PDDocumentNameDictionary(document.getDocumentCatalog());
    names.setEmbeddedFiles(tree);
    document.getDocumentCatalog().setNames(names);
  }

  private PDComplexFileSpecification specification(PDDocument document)
    throws IOException {
    var file = new PDEmbeddedFile(document, new ByteArrayInputStream(xml));
    file.setSubtype("text/xml");
    var specification = new PDComplexFileSpecification();
    specification.setFile("factur-x.xml");
    specification.setFileUnicode("factur-x.xml");
    specification.setEmbeddedFile(file);
    specification.setEmbeddedFileUnicode(file);
    return specification;
  }
}