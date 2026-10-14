package de.lukasbreuer.clarity.api.business.mail;

import de.lukasbreuer.clarity.api.business.invoice.Invoice;
import de.lukasbreuer.clarity.api.business.invoice.InvoiceDocument;
import de.lukasbreuer.clarity.api.business.invoice.InvoiceDocumentRepository;
import de.lukasbreuer.clarity.api.business.invoice.InvoiceRepository;
import de.lukasbreuer.clarity.api.business.invoice.reading.InvoiceFile;
import de.lukasbreuer.clarity.api.business.invoice.reading.RecognisedInvoice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * Stores an invoice from a mail together with its file. A file the space
 * has already, recognised by its SHA-256, is not stored again. An invoice
 * without a number always needs the owner's review.
 */
@Component
@RequiredArgsConstructor
final class InvoiceArchive {
  private static final int MAXIMUM_NAME_LENGTH = 200;
  private static final int MAXIMUM_MAIL_FIELD_LENGTH = 500;

  private final InvoiceRepository invoiceRepository;
  private final InvoiceDocumentRepository documentRepository;

  /**
   * @return Empty if the space has this file already
   */
  Optional<Invoice> store(
    MailDelivery delivery, RecognisedInvoice invoice, InvoiceFile file
  ) {
    var hash = sha256(file.content());
    if (documentRepository.existsBySpaceIdAndSha256(delivery.spaceId(), hash)
      .join()) {
      return Optional.empty();
    }
    var document = documentRepository.save(InvoiceDocument.create(
      UUID.randomUUID(), delivery.spaceId(), documentName(file),
      file.safeContentType(), hash, file.content())).join();
    return Optional.of(invoiceRepository.save(invoice(delivery,
      delivery.withParties(invoice), document, file)).join());
  }

  private Invoice invoice(
    MailDelivery delivery, RecognisedInvoice recognised,
    InvoiceDocument document, InvoiceFile file
  ) {
    var fields = recognised.fields();
    var mail = delivery.mail();
    return Invoice.create(UUID.randomUUID(), delivery.spaceId(),
      delivery.direction(), fields.number(), fields.issuer(),
      fields.recipient(), fields.iban(), fields.currency(),
      fields.netAmount(), fields.taxAmount(), fields.grossAmount(),
      fields.invoiceDate(), fields.dueDate(), recognised.format(),
      recognised.needsReview() || fields.number().isEmpty(), false, null,
      null, delivery.mailbox().id(), mailField(mail.subject()),
      mailField(mail.sender()), mail.receivedAt(), document.id(),
      document.name(), document.contentType(), (long) file.content().length,
      System.currentTimeMillis());
  }

  private String mailField(String value) {
    return TruncatedText.of(value, MAXIMUM_MAIL_FIELD_LENGTH).value();
  }

  /**
   * The sender's file name without path separators, control characters or
   * quotes, since it ends up in a download header.
   */
  private String documentName(InvoiceFile file) {
    var name = file.name().replaceAll("[\\\\/\\p{Cntrl}\"]", "_").trim();
    return name.isEmpty() ? "invoice" :
      TruncatedText.of(name, MAXIMUM_NAME_LENGTH).value();
  }

  private String sha256(byte[] content) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
        .digest(content));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }
}