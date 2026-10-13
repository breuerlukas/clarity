package de.lukasbreuer.clarity.api.business.invoice;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The fields of an invoice, whether read from a file or entered by the
 * owner. The IBAN is the account the issuer wants the money on; the net and
 * tax amounts and the due date are often missing.
 */
public record InvoiceFields(
  String number, String issuer, String recipient, String iban,
  String currency, BigDecimal netAmount, BigDecimal taxAmount,
  BigDecimal grossAmount, LocalDate invoiceDate, LocalDate dueDate
) {
  public InvoiceFields withParties(String issuer, String recipient) {
    return new InvoiceFields(number, issuer, recipient, iban, currency,
      netAmount, taxAmount, grossAmount, invoiceDate, dueDate);
  }
}
