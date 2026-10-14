package de.lukasbreuer.clarity.api.business.mail;

import de.lukasbreuer.clarity.api.business.invoice.InvoiceFormat;
import de.lukasbreuer.clarity.api.business.invoice.matching.InvoiceNumber;
import de.lukasbreuer.clarity.api.business.invoice.reading.InvoiceFile;
import de.lukasbreuer.clarity.api.business.invoice.reading.RecognisedInvoice;

/**
 * An attachment of a mail together with the invoice read from it.
 */
record FoundInvoice(InvoiceFile file, RecognisedInvoice invoice) {
  String number() {
    return invoice.fields().number();
  }

  boolean isElectronic() {
    return invoice.format().isElectronic();
  }

  boolean isXRechnung() {
    return invoice.format() == InvoiceFormat.XRECHNUNG;
  }

  /**
   * Compares the numbers without spaces, dashes and case, the way a sender
   * may write the same number differently.
   */
  boolean hasNumber(String otherNumber) {
    return InvoiceNumber.of(number()).sameAs(otherNumber);
  }
}