package de.lukasbreuer.clarity.api.business.invoice.reading;

import de.lukasbreuer.clarity.api.business.invoice.InvoiceFields;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * An invoice or credit note in OASIS UBL syntax (XRechnung UBL). The gross
 * amount is what is due, after any prepayment. A missing mandatory element
 * throws.
 */
@RequiredArgsConstructor(staticName = "of")
final class UblInvoice {
  private final XmlElement root;

  InvoiceFields fields() {
    var currency = root.childText("DocumentCurrencyCode").orElse("EUR");
    var total = root.child("LegalMonetaryTotal").orElseThrow();
    var paymentMeans = root.child("PaymentMeans");
    return new InvoiceFields(root.childText("ID").orElseThrow(),
      party("AccountingSupplierParty"), party("AccountingCustomerParty"),
      iban(paymentMeans).orElse(null), currency,
      total.childAmount("TaxExclusiveAmount").orElse(null),
      tax().orElse(null), gross(total), issueDate(),
      dueDate(paymentMeans).orElse(null));
  }

  /**
   * The party's trading name, otherwise its registered legal name.
   */
  private String party(String role) {
    var party = root.child(role).flatMap(element -> element.child("Party"));
    return party.flatMap(element -> element.child("PartyName"))
      .flatMap(name -> name.childText("Name"))
      .or(() -> party.flatMap(element -> element.child("PartyLegalEntity"))
        .flatMap(entity -> entity.childText("RegistrationName")))
      .orElse("");
  }

  private Optional<String> iban(Optional<XmlElement> paymentMeans) {
    return paymentMeans
      .flatMap(means -> means.child("PayeeFinancialAccount"))
      .flatMap(account -> account.childText("ID"))
      .map(Iban::of)
      .filter(Iban::isValid)
      .map(Iban::normalized);
  }

  private Optional<BigDecimal> tax() {
    return root.child("TaxTotal")
      .flatMap(taxTotal -> taxTotal.childAmount("TaxAmount"));
  }

  private BigDecimal gross(XmlElement total) {
    return total.childAmount("PayableAmount")
      .or(() -> total.childAmount("TaxInclusiveAmount"))
      .orElseThrow();
  }

  private LocalDate issueDate() {
    return LocalDate.parse(root.childText("IssueDate").orElseThrow());
  }

  private Optional<LocalDate> dueDate(Optional<XmlElement> paymentMeans) {
    return root.childText("DueDate")
      .or(() -> paymentMeans.flatMap(means -> means.childText(
        "PaymentDueDate")))
      .map(LocalDate::parse);
  }
}