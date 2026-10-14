package de.lukasbreuer.clarity.api.business.invoice.reading;

import de.lukasbreuer.clarity.api.business.invoice.InvoiceFields;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * An invoice in UN/CEFACT CII syntax: XRechnung CII, and the XML inside
 * ZUGFeRD and Factur-X PDFs. The gross amount is what is due, after any
 * prepayment. A missing mandatory element throws.
 */
@RequiredArgsConstructor(staticName = "of")
final class CiiInvoice {
  private static final DateTimeFormatter DATE =
    DateTimeFormatter.ofPattern("yyyyMMdd");
  private static final String SUMMATION =
    "SpecifiedTradeSettlementHeaderMonetarySummation";

  private final XmlElement root;

  InvoiceFields fields() {
    var exchanged = root.child("ExchangedDocument").orElseThrow();
    var agreement = transactionPart("ApplicableHeaderTradeAgreement");
    var settlement = transactionPart("ApplicableHeaderTradeSettlement");
    var summation = settlement.child(SUMMATION).orElseThrow();
    var currency = settlement.childText("InvoiceCurrencyCode").orElse("EUR");
    return new InvoiceFields(exchanged.childText("ID").orElseThrow(),
      partyName(agreement, "SellerTradeParty"),
      partyName(agreement, "BuyerTradeParty"), iban(settlement), currency,
      summation.childAmount("TaxBasisTotalAmount").orElse(null),
      tax(summation, currency).orElse(null), gross(summation),
      issueDate(exchanged), dueDate(settlement).orElse(null));
  }

  private XmlElement transactionPart(String localName) {
    return root.child("SupplyChainTradeTransaction").orElseThrow()
      .child(localName).orElseThrow();
  }

  private String partyName(XmlElement agreement, String party) {
    return agreement.child(party)
      .flatMap(element -> element.childText("Name")).orElse("");
  }

  private String iban(XmlElement settlement) {
    return settlement.descendant("PayeePartyCreditorFinancialAccount")
      .flatMap(account -> account.childText("IBANID"))
      .map(iban -> Iban.of(iban).normalized()).orElse(null);
  }

  private BigDecimal gross(XmlElement summation) {
    return summation.childAmount("DuePayableAmount")
      .or(() -> summation.childAmount("GrandTotalAmount"))
      .orElseThrow();
  }

  /**
   * The tax total in the invoice currency; invoices in another currency may
   * state it in euros as well.
   */
  private Optional<BigDecimal> tax(XmlElement summation, String currency) {
    return summation.children("TaxTotalAmount").stream()
      .filter(element -> isInCurrency(element, currency))
      .findFirst()
      .flatMap(XmlElement::amount);
  }

  private boolean isInCurrency(XmlElement amount, String currency) {
    var amountCurrency = amount.attribute("currencyID");
    return currency.equals(amountCurrency) || amountCurrency.isEmpty();
  }

  private LocalDate issueDate(XmlElement exchanged) {
    return exchanged.child("IssueDateTime").flatMap(this::date)
      .orElseThrow();
  }

  private Optional<LocalDate> dueDate(XmlElement settlement) {
    return settlement.descendant("SpecifiedTradePaymentTerms")
      .flatMap(terms -> terms.child("DueDateDateTime"))
      .flatMap(this::date);
  }

  private Optional<LocalDate> date(XmlElement dateTime) {
    return dateTime.childText("DateTimeString")
      .map(value -> LocalDate.parse(value.substring(0, 8), DATE));
  }
}