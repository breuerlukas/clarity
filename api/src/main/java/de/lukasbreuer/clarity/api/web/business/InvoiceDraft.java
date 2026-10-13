package de.lukasbreuer.clarity.api.web.business;

import de.lukasbreuer.clarity.api.business.invoice.InvoiceDirection;
import de.lukasbreuer.clarity.api.business.invoice.InvoiceFields;
import de.lukasbreuer.clarity.api.business.invoice.reading.Iban;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * The fields of an invoice as the owner corrected them, checked: a number,
 * both parties, a valid IBAN or none, a three letter currency, a gross
 * amount above zero with at most cents (net and tax may be unknown) and a
 * due date not before the invoice date.
 */
record InvoiceDraft(InvoiceDirection direction, InvoiceFields fields) {
  private static final BigDecimal MAXIMUM_AMOUNT = new BigDecimal("1000000000");

  static Optional<InvoiceDraft> parse(ApiRequestBody body) {
    try {
      var iban = Iban.normalize(body.getString("iban", 34));
      var dueDate = body.getString("due_date", 10);
      var draft = new InvoiceDraft(
        InvoiceDirection.find(body.getString("direction", 16)).orElseThrow(),
        new InvoiceFields(body.getString("number", 60).trim(),
          body.getString("issuer", 200).trim(),
          body.getString("recipient", 200).trim(),
          iban.isEmpty() ? null : iban,
          body.getString("currency", 3).toUpperCase(),
          optionalAmount(body.getString("net_amount", 24)),
          optionalAmount(body.getString("tax_amount", 24)),
          new BigDecimal(body.getString("gross_amount", 24)),
          LocalDate.parse(body.getString("invoice_date", 10)),
          dueDate.isEmpty() ? null : LocalDate.parse(dueDate)));
      return draft.isValid() ? Optional.of(draft) : Optional.empty();
    } catch (Exception exception) {
      return Optional.empty();
    }
  }

  private static BigDecimal optionalAmount(String value) {
    return value.isBlank() ? null : new BigDecimal(value.trim());
  }

  private boolean isValid() {
    return !fields.number().isEmpty() && !fields.issuer().isEmpty() &&
      !fields.recipient().isEmpty() &&
      (fields.iban() == null || Iban.isValid(fields.iban())) &&
      fields.currency().matches("[A-Z]{3}") &&
      isAmount(fields.grossAmount(), false) &&
      isAmount(fields.netAmount(), true) &&
      isAmount(fields.taxAmount(), true) &&
      (fields.dueDate() == null ||
        !fields.dueDate().isBefore(fields.invoiceDate()));
  }

  private static boolean isAmount(BigDecimal amount, boolean optional) {
    if (amount == null) {
      return optional;
    }
    return amount.signum() >= (optional ? 0 : 1) &&
      amount.stripTrailingZeros().scale() <= 2 &&
      amount.compareTo(MAXIMUM_AMOUNT) < 0;
  }
}
