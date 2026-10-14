package de.lukasbreuer.clarity.api.web.business;

import de.lukasbreuer.clarity.api.business.invoice.InvoiceDirection;
import de.lukasbreuer.clarity.api.business.invoice.InvoiceFields;
import de.lukasbreuer.clarity.api.business.invoice.reading.Iban;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;

import java.math.BigDecimal;
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
      var form = InvoiceForm.of(body);
      return Optional.of(new InvoiceDraft(form.direction(), form.fields()))
        .filter(InvoiceDraft::isValid);
    } catch (Exception exception) {
      return Optional.empty();
    }
  }

  private boolean isValid() {
    return hasParties() && hasValidIban() &&
      fields.currency().matches("[A-Z]{3}") && hasValidAmounts() &&
      hasValidDueDate();
  }

  private boolean hasParties() {
    return !fields.number().isEmpty() && !fields.issuer().isEmpty() &&
      !fields.recipient().isEmpty();
  }

  private boolean hasValidIban() {
    return fields.iban() == null || Iban.of(fields.iban()).isValid();
  }

  private boolean hasValidAmounts() {
    return isAmount(fields.grossAmount(), false) &&
      isAmount(fields.netAmount(), true) &&
      isAmount(fields.taxAmount(), true);
  }

  private boolean hasValidDueDate() {
    return fields.dueDate() == null ||
      !fields.dueDate().isBefore(fields.invoiceDate());
  }

  /**
   * @param optional Whether the amount may be unknown or zero
   */
  private boolean isAmount(BigDecimal amount, boolean optional) {
    if (amount == null) {
      return optional;
    }
    return amount.signum() >= (optional ? 0 : 1) &&
      amount.stripTrailingZeros().scale() <= 2 &&
      amount.compareTo(MAXIMUM_AMOUNT) < 0;
  }
}