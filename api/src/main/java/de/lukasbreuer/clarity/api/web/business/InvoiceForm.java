package de.lukasbreuer.clarity.api.web.business;

import de.lukasbreuer.clarity.api.business.invoice.InvoiceDirection;
import de.lukasbreuer.clarity.api.business.invoice.InvoiceFields;
import de.lukasbreuer.clarity.api.business.invoice.reading.Iban;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

/**
 * Reads the fields of an invoice from the request as the panel sends them:
 * amounts as strings, and an empty IBAN, net amount, tax amount or due date
 * for one that is unknown. Throws on anything that does not parse.
 */
@RequiredArgsConstructor(staticName = "of")
final class InvoiceForm {
  private final ApiRequestBody body;

  InvoiceDirection direction() {
    return InvoiceDirection.find(body.getString("direction", 16))
      .orElseThrow();
  }

  InvoiceFields fields() {
    return new InvoiceFields(text("number", 60), text("issuer", 200),
      text("recipient", 200), iban(),
      body.getString("currency", 3).toUpperCase(Locale.ROOT),
      optionalAmount("net_amount"), optionalAmount("tax_amount"),
      new BigDecimal(body.getString("gross_amount", 24)),
      LocalDate.parse(body.getString("invoice_date", 10)), dueDate());
  }

  private String text(String key, int maxLength) {
    return body.getString(key, maxLength).trim();
  }

  private String iban() {
    var iban = Iban.of(body.getString("iban", 34)).normalized();
    return iban.isEmpty() ? null : iban;
  }

  private BigDecimal optionalAmount(String key) {
    var value = body.getString(key, 24);
    return value.isBlank() ? null : new BigDecimal(value.trim());
  }

  private LocalDate dueDate() {
    var value = body.getString("due_date", 10);
    return value.isEmpty() ? null : LocalDate.parse(value);
  }
}