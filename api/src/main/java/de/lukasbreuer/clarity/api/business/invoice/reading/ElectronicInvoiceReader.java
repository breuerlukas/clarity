package de.lukasbreuer.clarity.api.business.invoice.reading;

import de.lukasbreuer.clarity.api.business.invoice.InvoiceFields;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Reads the two XML syntaxes e-invoices come in: UN/CEFACT CII (XRechnung
 * CII, and the XML inside ZUGFeRD and Factur-X PDFs) and OASIS UBL (XRechnung
 * UBL, invoices and credit notes). Elements are found by local name so
 * namespace prefixes do not matter. The gross amount is what is due, after
 * any prepayment.
 */
public final class ElectronicInvoiceReader {
  private static final DateTimeFormatter CII_DATE =
    DateTimeFormatter.ofPattern("yyyyMMdd");

  private ElectronicInvoiceReader() {
  }

  public static Optional<InvoiceFields> read(byte[] xml) {
    return SecureXml.parse(xml).flatMap(ElectronicInvoiceReader::read);
  }

  private static Optional<InvoiceFields> read(Document document) {
    var root = document.getDocumentElement();
    try {
      return switch (root.getLocalName()) {
        case "CrossIndustryInvoice" -> Optional.of(readCii(root));
        case "Invoice", "CreditNote" -> Optional.of(readUbl(root));
        default -> Optional.empty();
      };
    } catch (RuntimeException exception) {
      return Optional.empty();
    }
  }

  private static InvoiceFields readCii(Element root) {
    var exchanged = child(root, "ExchangedDocument").orElseThrow();
    var transaction = child(root, "SupplyChainTradeTransaction").orElseThrow();
    var agreement = child(transaction, "ApplicableHeaderTradeAgreement")
      .orElseThrow();
    var settlement = child(transaction, "ApplicableHeaderTradeSettlement")
      .orElseThrow();
    var currency = text(child(settlement, "InvoiceCurrencyCode")).orElse("EUR");
    var summation = child(settlement,
      "SpecifiedTradeSettlementHeaderMonetarySummation").orElseThrow();
    var gross = amount(child(summation, "DuePayableAmount"))
      .or(() -> amount(child(summation, "GrandTotalAmount")))
      .orElseThrow();
    var tax = children(summation, "TaxTotalAmount").stream()
      .filter(element -> currency.equals(element.getAttribute("currencyID")) ||
        element.getAttribute("currencyID").isEmpty())
      .findFirst()
      .flatMap(element -> amount(Optional.of(element)));
    var iban = descendant(settlement, "PayeePartyCreditorFinancialAccount")
      .flatMap(account -> text(child(account, "IBANID")));
    var dueDate = descendant(settlement, "SpecifiedTradePaymentTerms")
      .flatMap(terms -> child(terms, "DueDateDateTime"))
      .flatMap(ElectronicInvoiceReader::ciiDate);
    return new InvoiceFields(
      text(child(exchanged, "ID")).orElseThrow(),
      partyName(agreement, "SellerTradeParty"),
      partyName(agreement, "BuyerTradeParty"),
      iban.map(Iban::normalize).orElse(null), currency,
      amount(child(summation, "TaxBasisTotalAmount")).orElse(null),
      tax.orElse(null), gross,
      child(exchanged, "IssueDateTime")
        .flatMap(ElectronicInvoiceReader::ciiDate).orElseThrow(),
      dueDate.orElse(null));
  }

  private static String partyName(Element agreement, String party) {
    return child(agreement, party).flatMap(element -> text(child(element,
      "Name"))).orElse("");
  }

  private static Optional<LocalDate> ciiDate(Element dateTime) {
    return text(child(dateTime, "DateTimeString"))
      .map(value -> LocalDate.parse(value.substring(0, 8), CII_DATE));
  }

  private static InvoiceFields readUbl(Element root) {
    var currency = text(child(root, "DocumentCurrencyCode")).orElse("EUR");
    var total = child(root, "LegalMonetaryTotal").orElseThrow();
    var gross = amount(child(total, "PayableAmount"))
      .or(() -> amount(child(total, "TaxInclusiveAmount")))
      .orElseThrow();
    var paymentMeans = child(root, "PaymentMeans");
    var iban = paymentMeans
      .flatMap(means -> child(means, "PayeeFinancialAccount"))
      .flatMap(account -> text(child(account, "ID")))
      .filter(id -> Iban.isValid(Iban.normalize(id)));
    var dueDate = text(child(root, "DueDate"))
      .or(() -> paymentMeans.flatMap(means -> text(child(means,
        "PaymentDueDate"))))
      .map(LocalDate::parse);
    return new InvoiceFields(
      text(child(root, "ID")).orElseThrow(),
      ublParty(root, "AccountingSupplierParty"),
      ublParty(root, "AccountingCustomerParty"),
      iban.map(Iban::normalize).orElse(null), currency,
      amount(child(total, "TaxExclusiveAmount")).orElse(null),
      child(root, "TaxTotal").flatMap(taxTotal -> amount(child(taxTotal,
        "TaxAmount"))).orElse(null),
      gross, LocalDate.parse(text(child(root, "IssueDate")).orElseThrow()),
      dueDate.orElse(null));
  }

  private static String ublParty(Element root, String role) {
    var party = child(root, role).flatMap(element -> child(element, "Party"));
    return party.flatMap(element -> child(element, "PartyName"))
      .flatMap(name -> text(child(name, "Name")))
      .or(() -> party.flatMap(element -> child(element, "PartyLegalEntity"))
        .flatMap(entity -> text(child(entity, "RegistrationName"))))
      .orElse("");
  }

  private static Optional<BigDecimal> amount(Optional<Element> element) {
    return text(element).map(BigDecimal::new);
  }

  private static Optional<String> text(Optional<Element> element) {
    return element.map(Node::getTextContent).map(String::trim)
      .filter(value -> !value.isEmpty());
  }

  private static Optional<Element> child(Element parent, String localName) {
    return children(parent, localName).stream().findFirst();
  }

  private static List<Element> children(Element parent, String localName) {
    var children = new ArrayList<Element>();
    for (var node = parent.getFirstChild(); node != null;
      node = node.getNextSibling()) {
      if (node instanceof Element element &&
        localName.equals(element.getLocalName())) {
        children.add(element);
      }
    }
    return children;
  }

  private static Optional<Element> descendant(
    Element parent, String localName
  ) {
    var elements = parent.getElementsByTagNameNS("*", localName);
    return elements.getLength() == 0 ? Optional.empty() :
      Optional.of((Element) elements.item(0));
  }
}
