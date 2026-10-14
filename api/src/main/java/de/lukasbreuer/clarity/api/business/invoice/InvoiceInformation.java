package de.lukasbreuer.clarity.api.business.invoice;

import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * An invoice as the panel reads it: its fields, where it came from, the
 * payments that pay it and where it stands today.
 */
@RequiredArgsConstructor(staticName = "of")
final class InvoiceInformation {
  private final Invoice invoice;
  private final InvoiceBalance balance;
  private final LocalDate today;

  Map<String, Object> toMap() {
    var information = new HashMap<String, Object>();
    putFields(information);
    putAmounts(information);
    putReview(information);
    information.put("mail", mail());
    information.put("document", document());
    return information;
  }

  private void putFields(Map<String, Object> information) {
    information.put("id", invoice.id());
    information.put("space", invoice.spaceId());
    information.put("direction", invoice.direction().name());
    information.put("number", invoice.number());
    information.put("issuer", invoice.issuer());
    information.put("recipient", invoice.recipient());
    information.put("iban", invoice.iban());
    information.put("invoice_date", invoice.invoiceDate().toString());
    information.put("due_date", Objects.toString(invoice.dueDate(), null));
    information.put("created_at", invoice.createdAt());
  }

  private void putAmounts(Map<String, Object> information) {
    information.put("currency", invoice.currency());
    information.put("net_amount", invoice.netAmount());
    information.put("tax_amount", invoice.taxAmount());
    information.put("gross_amount", invoice.grossAmount());
    information.put("payments", balance.accepted().stream()
      .map(InvoicePayment::information).toList());
    information.put("paid_amount", balance.paid());
    information.put("open_amount", balance.open());
  }

  private void putReview(Map<String, Object> information) {
    information.put("format", invoice.format().name());
    information.put("needs_review", invoice.needsReview());
    information.put("settled", invoice.settled());
    information.put("announced_at",
      Objects.toString(invoice.announcedAt(), null));
    information.put("announced_amount", invoice.announcedAmount());
    information.put("status", balance.status(today).name());
  }

  private Map<String, Object> mail() {
    if (invoice.mailSubject() == null) {
      return null;
    }
    return Map.of("subject", invoice.mailSubject(),
      "sender", Objects.requireNonNullElse(invoice.mailSender(), ""),
      "received_at", Objects.requireNonNullElse(invoice.mailReceivedAt(), 0L));
  }

  private Map<String, Object> document() {
    if (invoice.documentId() == null) {
      return null;
    }
    return Map.of("name", invoice.documentName(),
      "content_type", invoice.documentContentType(),
      "size", invoice.documentSize());
  }
}