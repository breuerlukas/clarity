package de.lukasbreuer.clarity.api.business.invoice.reading;

import de.lukasbreuer.clarity.api.business.invoice.InvoiceFields;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import java.util.Optional;

/**
 * Reads the two XML syntaxes e-invoices come in: UN/CEFACT CII (XRechnung
 * CII, and the XML inside ZUGFeRD and Factur-X PDFs) and OASIS UBL (XRechnung
 * UBL, invoices and credit notes). Any other XML, or an invoice missing a
 * mandatory field, is no invoice.
 */
@Component
@RequiredArgsConstructor(staticName = "create")
public final class ElectronicInvoiceReader {
  private final SecureXml secureXml;

  public Optional<InvoiceFields> read(byte[] xml) {
    return secureXml.parse(xml).flatMap(this::readDocument);
  }

  private Optional<InvoiceFields> readDocument(Document document) {
    var root = XmlElement.of(document.getDocumentElement());
    try {
      return switch (root.localName()) {
        case "CrossIndustryInvoice" -> Optional.of(CiiInvoice.of(root)
          .fields());
        case "Invoice", "CreditNote" -> Optional.of(UblInvoice.of(root)
          .fields());
        default -> Optional.empty();
      };
    } catch (RuntimeException exception) {
      return Optional.empty();
    }
  }
}