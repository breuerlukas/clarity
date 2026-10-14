package de.lukasbreuer.clarity.api.business.invoice.reading;

import de.lukasbreuer.clarity.api.business.invoice.InvoiceFields;
import de.lukasbreuer.clarity.api.business.invoice.InvoiceFormat;

/**
 * An invoice read from a file. Fields read from text instead of an e-invoice
 * need the owner's review; the issuer or recipient may then be empty, to be
 * filled in from the mail.
 */
public record RecognisedInvoice(
  InvoiceFields fields, InvoiceFormat format, boolean needsReview
) {
  public RecognisedInvoice withParties(String issuer, String recipient) {
    return new RecognisedInvoice(fields.withParties(issuer, recipient),
      format, needsReview);
  }
}