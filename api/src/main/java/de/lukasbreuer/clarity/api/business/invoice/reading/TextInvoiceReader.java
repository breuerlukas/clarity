package de.lukasbreuer.clarity.api.business.invoice.reading;

import de.lukasbreuer.clarity.api.business.invoice.InvoiceFields;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Guesses the fields of an invoice from its text, in German or English. Text
 * only counts as an invoice when it names a total and either an invoice
 * number or a heading that says invoice (and no other heading such as an
 * offer or a credit note); everything read here is a guess the owner
 * confirms. The parties are left empty, the import takes them from the
 * mail.
 */
@Component
@RequiredArgsConstructor(staticName = "create")
public final class TextInvoiceReader {
  /**
   * @param received When the mail arrived, the invoice date if the text has
   *                 none
   */
  public Optional<InvoiceFields> read(String text, LocalDate received) {
    var invoiceText = InvoiceText.of(text);
    if (!invoiceText.isInvoice()) {
      return Optional.empty();
    }
    return invoiceText.gross().filter(gross -> gross.signum() > 0)
      .map(gross -> fields(invoiceText, gross, received));
  }

  private InvoiceFields fields(
    InvoiceText text, BigDecimal gross, LocalDate received
  ) {
    var invoiceDate = text.invoiceDate().orElse(received);
    return new InvoiceFields(text.number().orElse(""), "", "",
      text.iban().orElse(null), text.currency(),
      text.net(gross).orElse(null), text.tax(gross).orElse(null), gross,
      invoiceDate, text.dueDate(invoiceDate).orElse(null));
  }
}