package de.lukasbreuer.clarity.api.business.mail;

import de.lukasbreuer.clarity.api.business.invoice.Invoice;
import de.lukasbreuer.clarity.api.business.invoice.InvoiceDirection;
import de.lukasbreuer.clarity.api.business.invoice.InvoiceDocument;
import de.lukasbreuer.clarity.api.business.invoice.InvoiceDocumentRepository;
import de.lukasbreuer.clarity.api.business.invoice.InvoiceFormat;
import de.lukasbreuer.clarity.api.business.invoice.InvoiceRepository;
import de.lukasbreuer.clarity.api.business.invoice.matching.InvoiceNumber;
import de.lukasbreuer.clarity.api.business.invoice.reading.InvoiceFile;
import de.lukasbreuer.clarity.api.business.invoice.reading.InvoiceRecognition;
import de.lukasbreuer.clarity.api.business.invoice.reading.RecognisedInvoice;
import de.lukasbreuer.clarity.api.space.Space;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Turns one mail into invoices of a space. Mails in the incoming folder are
 * invoices to the business, unless they are payment advices, which mark the
 * invoices they name as announced; mails in the outgoing folder are invoices
 * the business sent. When a mail carries the same invoice twice (an
 * XRechnung next to its PDF, say), the e-invoice is kept with the PDF as the
 * file to read. A file imported before, or an invoice number already known
 * for that direction, is skipped.
 */
@Component
@RequiredArgsConstructor
public final class MailImport {
  private final InvoiceRecognition recognition;
  private final InvoiceRepository invoiceRepository;
  private final InvoiceDocumentRepository documentRepository;

  private record Found(InvoiceFile file, RecognisedInvoice invoice) {
  }

  public void store(
    Space space, Mailbox mailbox, InvoiceDirection direction,
    ReceivedMail mail
  ) {
    var received = mail.receivedOn();
    if (direction == InvoiceDirection.INCOMING &&
      PaymentAdvice.isAdvice(mail)) {
      announce(space, mail, received);
      return;
    }
    var found = new ArrayList<Found>();
    for (var file : mail.attachments()) {
      recognition.recognise(file, received).invoice()
        .ifPresent(invoice -> found.add(new Found(file, invoice)));
    }
    var known = new ArrayList<>(invoiceRepository.findBySpaceId(space.id())
      .join());
    for (var entry : withoutDuplicates(found)) {
      store(space, mailbox, direction, mail, entry, found, known);
    }
  }

  private void store(
    Space space, Mailbox mailbox, InvoiceDirection direction,
    ReceivedMail mail, Found entry, List<Found> found, List<Invoice> known
  ) {
    var number = entry.invoice().fields().number();
    if (!number.isEmpty() && known.stream().anyMatch(invoice ->
      invoice.direction() == direction &&
        sameNumber(invoice.number(), number))) {
      return;
    }
    var file = readableFile(entry, found);
    var hash = sha256(file.content());
    if (documentRepository.existsBySpaceIdAndSha256(space.id(), hash).join()) {
      return;
    }
    var document = documentRepository.save(InvoiceDocument.create(
      UUID.randomUUID(), space.id(), fileName(file), file.safeContentType(),
      hash, file.content())).join();
    var recognised = withParties(entry.invoice(), space, direction, mail);
    var fields = recognised.fields();
    var invoice = invoiceRepository.save(Invoice.create(UUID.randomUUID(),
      space.id(), direction, fields.number(), fields.issuer(),
      fields.recipient(), fields.iban(), fields.currency(),
      fields.netAmount(), fields.taxAmount(), fields.grossAmount(),
      fields.invoiceDate(), fields.dueDate(), recognised.format(),
      recognised.needsReview() || fields.number().isEmpty(), false, null,
      null, mailbox.id(), cut(mail.subject(), 500), cut(mail.sender(), 500),
      mail.receivedAt(), document.id(), document.name(),
      document.contentType(), (long) file.content().length,
      System.currentTimeMillis())).join();
    known.add(invoice);
  }

  /**
   * One entry per invoice number; an e-invoice wins over a text read of the
   * same invoice.
   */
  private List<Found> withoutDuplicates(List<Found> found) {
    var kept = new ArrayList<Found>();
    for (var entry : found) {
      var number = entry.invoice().fields().number();
      var twin = kept.stream().filter(other -> !number.isEmpty() &&
        sameNumber(other.invoice().fields().number(), number)).findFirst();
      if (twin.isEmpty()) {
        kept.add(entry);
      } else if (entry.invoice().format().isElectronic() &&
        !twin.get().invoice().format().isElectronic()) {
        kept.set(kept.indexOf(twin.get()), entry);
      }
    }
    return kept;
  }

  /**
   * An XRechnung is XML nobody wants to read; if the mail also has a PDF of
   * the same invoice, that is the file kept.
   */
  private InvoiceFile readableFile(Found entry, List<Found> found) {
    if (entry.invoice().format() != InvoiceFormat.XRECHNUNG) {
      return entry.file();
    }
    var number = entry.invoice().fields().number();
    return found.stream()
      .filter(other -> other.file().isPdf())
      .filter(other -> sameNumber(other.invoice().fields().number(), number))
      .map(Found::file)
      .findFirst()
      .orElse(entry.file());
  }

  /**
   * Text based invoices have no parties; they come from the mail: the
   * sender issued an incoming invoice, the recipient gets an outgoing one,
   * and the business is the other side.
   */
  private RecognisedInvoice withParties(
    RecognisedInvoice invoice, Space space, InvoiceDirection direction,
    ReceivedMail mail
  ) {
    var fields = invoice.fields();
    var business = space.name();
    var issuer = fields.issuer().isEmpty() ?
      (direction == InvoiceDirection.INCOMING ? mail.sender() : business) :
      fields.issuer();
    var recipient = fields.recipient().isEmpty() ?
      (direction == InvoiceDirection.OUTGOING ? mail.recipient() : business) :
      fields.recipient();
    return invoice.withParties(cut(issuer, 200), cut(recipient, 200));
  }

  private void announce(Space space, ReceivedMail mail, LocalDate received) {
    var text = new StringBuilder(mail.subject()).append('\n')
      .append(mail.body());
    for (var file : mail.attachments()) {
      text.append('\n').append(recognition.recognise(file, received).text());
    }
    var announced = invoiceRepository.findBySpaceId(space.id()).join()
      .stream()
      .filter(invoice -> invoice.direction() == InvoiceDirection.OUTGOING)
      .filter(invoice -> invoice.announcedAt() == null)
      .filter(invoice -> InvoiceNumber.appearsIn(invoice.number(),
        text.toString()))
      .toList();
    announced.forEach(invoice -> invoice.announce(received, null));
    if (!announced.isEmpty()) {
      invoiceRepository.saveAll(announced).join();
    }
  }

  private static boolean sameNumber(String first, String second) {
    return InvoiceNumber.squash(first).equals(InvoiceNumber.squash(second));
  }

  private static String fileName(InvoiceFile file) {
    var name = file.name().replaceAll("[\\\\/\\p{Cntrl}\"]", "_").trim();
    return name.isEmpty() ? "invoice" : cut(name, 200);
  }

  private static String cut(String value, int length) {
    return value.substring(0, Math.min(value.length(), length));
  }

  private static String sha256(byte[] content) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
        .digest(content));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
