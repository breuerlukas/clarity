package de.lukasbreuer.clarity.api.business.mail;

import de.lukasbreuer.clarity.api.business.invoice.InvoiceDirection;
import de.lukasbreuer.clarity.api.business.invoice.InvoiceFields;
import de.lukasbreuer.clarity.api.business.invoice.reading.RecognisedInvoice;
import de.lukasbreuer.clarity.api.space.Space;

import java.time.LocalDate;
import java.util.UUID;

/**
 * One mail as it arrived in a folder of a space's mailbox: the incoming
 * folder holds invoices to the business, the outgoing one invoices the
 * business sent.
 */
record MailDelivery(
  Space space, Mailbox mailbox, InvoiceDirection direction, ReceivedMail mail
) {
  private static final int MAXIMUM_PARTY_LENGTH = 200;

  UUID spaceId() {
    return space.id();
  }

  LocalDate received() {
    return mail.receivedOn();
  }

  /**
   * Only customers send payment advices, so they arrive in the incoming
   * folder.
   */
  boolean isAdvice() {
    return direction == InvoiceDirection.INCOMING &&
      PaymentAdvice.of(mail).isAdvice();
  }

  /**
   * Text based invoices have no parties; they come from the mail: the
   * sender issued an incoming invoice, the recipient gets an outgoing one,
   * and the business is the other side.
   */
  RecognisedInvoice withParties(RecognisedInvoice invoice) {
    var fields = invoice.fields();
    return invoice.withParties(party(issuer(fields)),
      party(recipient(fields)));
  }

  private String party(String name) {
    return TruncatedText.of(name, MAXIMUM_PARTY_LENGTH).value();
  }

  private String issuer(InvoiceFields fields) {
    if (!fields.issuer().isEmpty()) {
      return fields.issuer();
    }
    return direction == InvoiceDirection.INCOMING ? mail.sender() :
      space.name();
  }

  private String recipient(InvoiceFields fields) {
    if (!fields.recipient().isEmpty()) {
      return fields.recipient();
    }
    return direction == InvoiceDirection.OUTGOING ? mail.recipient() :
      space.name();
  }
}