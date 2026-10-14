package de.lukasbreuer.clarity.api.business.mail;

import de.lukasbreuer.clarity.api.business.invoice.Invoice;
import de.lukasbreuer.clarity.api.business.invoice.InvoiceDirection;
import de.lukasbreuer.clarity.api.business.invoice.InvoiceRepository;
import de.lukasbreuer.clarity.api.business.invoice.reading.InvoiceFile;
import de.lukasbreuer.clarity.api.business.invoice.reading.InvoiceRecognition;
import de.lukasbreuer.clarity.api.space.Space;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Turns one mail into invoices of a space. Mails in the incoming folder are
 * invoices to the business, unless they are payment advices, which mark the
 * invoices they name as announced; mails in the outgoing folder are invoices
 * the business sent. When a mail carries the same invoice twice, the
 * e-invoice is kept with the PDF as the file to read. A file imported
 * before, or an invoice number already known for that direction, is skipped.
 */
@Component
@RequiredArgsConstructor
public final class MailImport {
  private final InvoiceRecognition recognition;
  private final InvoiceRepository invoiceRepository;
  private final InvoiceArchive archive;
  private final PaymentAdviceImport adviceImport;

  public void store(
    Space space, Mailbox mailbox, InvoiceDirection direction,
    ReceivedMail mail
  ) {
    var delivery = new MailDelivery(space, mailbox, direction, mail);
    if (delivery.isAdvice()) {
      adviceImport.announce(delivery);
      return;
    }
    store(delivery, FoundInvoices.of(recognise(delivery)));
  }

  private List<FoundInvoice> recognise(MailDelivery delivery) {
    return delivery.mail().attachments().stream()
      .flatMap(file -> recognise(delivery, file))
      .toList();
  }

  private Stream<FoundInvoice> recognise(
    MailDelivery delivery, InvoiceFile file
  ) {
    return recognition.recognise(file, delivery.received()).invoice()
      .map(invoice -> new FoundInvoice(file, invoice))
      .stream();
  }

  private void store(MailDelivery delivery, FoundInvoices found) {
    var known = new ArrayList<>(invoiceRepository
      .findBySpaceId(delivery.spaceId()).join());
    for (var entry : found.withoutDuplicates()) {
      if (!isKnown(delivery, entry, known)) {
        archive.store(delivery, entry.invoice(), found.readableFile(entry))
          .ifPresent(known::add);
      }
    }
  }

  private boolean isKnown(
    MailDelivery delivery, FoundInvoice entry, List<Invoice> known
  ) {
    return !entry.number().isEmpty() && known.stream().anyMatch(invoice ->
      invoice.direction() == delivery.direction() &&
        entry.hasNumber(invoice.number()));
  }
}