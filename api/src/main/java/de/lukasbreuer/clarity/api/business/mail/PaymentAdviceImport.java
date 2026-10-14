package de.lukasbreuer.clarity.api.business.mail;

import de.lukasbreuer.clarity.api.business.invoice.Invoice;
import de.lukasbreuer.clarity.api.business.invoice.InvoiceDirection;
import de.lukasbreuer.clarity.api.business.invoice.InvoiceRepository;
import de.lukasbreuer.clarity.api.business.invoice.matching.InvoiceNumber;
import de.lukasbreuer.clarity.api.business.invoice.reading.InvoiceRecognition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Marks the invoices a payment advice names as announced. The numbers are
 * looked for in the subject, the text and whatever can be read from the
 * attachments; only invoices the business sent and that were not announced
 * before count.
 */
@Component
@RequiredArgsConstructor
final class PaymentAdviceImport {
  private final InvoiceRecognition recognition;
  private final InvoiceRepository invoiceRepository;

  void announce(MailDelivery delivery) {
    var announced = announcedBy(delivery.spaceId(), adviceText(delivery));
    announced.forEach(invoice -> invoice.announce(delivery.received(), null));
    if (!announced.isEmpty()) {
      invoiceRepository.saveAll(announced).join();
    }
  }

  private List<Invoice> announcedBy(UUID spaceId, String text) {
    return invoiceRepository.findBySpaceId(spaceId).join().stream()
      .filter(invoice -> invoice.direction() == InvoiceDirection.OUTGOING)
      .filter(invoice -> invoice.announcedAt() == null)
      .filter(invoice -> InvoiceNumber.of(invoice.number()).appearsIn(text))
      .toList();
  }

  private String adviceText(MailDelivery delivery) {
    var mail = delivery.mail();
    var attachments = mail.attachments().stream().map(file ->
      recognition.recognise(file, delivery.received()).text());
    return Stream.concat(Stream.of(mail.subject(), mail.body()), attachments)
      .collect(Collectors.joining("\n"));
  }
}