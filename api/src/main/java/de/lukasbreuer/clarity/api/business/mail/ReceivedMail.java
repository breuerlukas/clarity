package de.lukasbreuer.clarity.api.business.mail;

import de.lukasbreuer.clarity.api.business.invoice.reading.InvoiceFile;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * What the import needs of a mail: who sent it to whom, when, its text and
 * the attachments that may be invoices.
 *
 * @param sender The sender's name, or the address without one
 * @param recipient The first recipient's name, or the address without one
 */
public record ReceivedMail(
  String subject, String sender, String recipient, long receivedAt,
  String body, List<InvoiceFile> attachments
) {
  public LocalDate receivedOn() {
    return LocalDate.ofInstant(Instant.ofEpochMilli(receivedAt),
      ZoneId.systemDefault());
  }
}