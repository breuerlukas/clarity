package de.lukasbreuer.clarity.api.business.invoice.reading;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDNameTreeNode;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The e-invoice XML a PDF carries as an attachment. ZUGFeRD names the file
 * zugferd-invoice.xml (1.0) or zugferd.xml, Factur-X and ZUGFeRD 2
 * factur-x.xml, XRechnung inside a PDF xrechnung.xml; any other embedded
 * XML is tried last.
 */
@RequiredArgsConstructor(staticName = "of")
final class EmbeddedInvoiceXml {
  private static final int MAXIMUM_XML_BYTES = 5 * 1024 * 1024;
  private static final List<String> KNOWN_NAMES = List.of("factur-x.xml",
    "zugferd-invoice.xml", "zugferd.xml", "xrechnung.xml");

  private final PDDocument document;

  Optional<byte[]> find() throws IOException {
    var names = document.getDocumentCatalog().getNames();
    if (names == null || names.getEmbeddedFiles() == null) {
      return Optional.empty();
    }
    var files = new ArrayList<NamedXml>();
    collect(names.getEmbeddedFiles(), files);
    return files.stream()
      .sorted(Comparator.comparing(NamedXml::isKnownName,
        Comparator.reverseOrder()))
      .map(NamedXml::content)
      .findFirst();
  }

  private record NamedXml(String name, byte[] content) {
    boolean isKnownName() {
      return KNOWN_NAMES.contains(name);
    }
  }

  private void collect(
    PDNameTreeNode<PDComplexFileSpecification> node, List<NamedXml> files
  ) throws IOException {
    if (node.getNames() != null) {
      for (var entry : node.getNames().entrySet()) {
        addIfXml(entry.getKey(), entry.getValue(), files);
      }
    }
    if (node.getKids() != null) {
      for (var kid : node.getKids()) {
        collect(kid, files);
      }
    }
  }

  private void addIfXml(
    String fileName, PDComplexFileSpecification specification,
    List<NamedXml> files
  ) throws IOException {
    var name = fileName.toLowerCase(Locale.ROOT);
    var file = embeddedFile(specification);
    if (name.endsWith(".xml") && file != null &&
      file.getLength() <= MAXIMUM_XML_BYTES) {
      files.add(new NamedXml(name, file.toByteArray()));
    }
  }

  private PDEmbeddedFile embeddedFile(
    PDComplexFileSpecification specification
  ) {
    var file = specification.getEmbeddedFileUnicode();
    return file != null ? file : specification.getEmbeddedFile();
  }
}