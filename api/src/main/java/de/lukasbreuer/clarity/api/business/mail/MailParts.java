package de.lukasbreuer.clarity.api.business.mail;

import de.lukasbreuer.clarity.api.business.invoice.reading.InvoiceFile;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeUtility;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Collects the text of one mail and the attachments that may be invoices:
 * PDFs, XML files and attached (not inline) images, each at most ten
 * megabytes. Mails forwarded as attachments are opened too, a few levels
 * deep. Used for a single mail only.
 */
@NoArgsConstructor(staticName = "create")
final class MailParts {
  private static final int MAXIMUM_ATTACHMENT_BYTES = 10 * 1024 * 1024;
  private static final int MAXIMUM_BODY_CHARACTERS = 100_000;
  private static final int MAXIMUM_DEPTH = 5;
  private static final int MAXIMUM_ATTACHMENTS = 20;

  private final StringBuilder plainBody = new StringBuilder();
  private final StringBuilder htmlBody = new StringBuilder();
  private final List<InvoiceFile> attachments = new ArrayList<>();

  void walk(Part part, int depth) throws MessagingException, IOException {
    if (depth > MAXIMUM_DEPTH) {
      return;
    }
    if (part.isMimeType("multipart/*") &&
      part.getContent() instanceof Multipart multipart) {
      walkParts(multipart, depth + 1);
    } else if (part.isMimeType("message/rfc822") &&
      part.getContent() instanceof Part nested) {
      walk(nested, depth + 1);
    } else {
      collect(part);
    }
  }

  /**
   * The plain text, or the HTML text without its tags if there is none.
   */
  String body() {
    return !plainBody.isEmpty() ? plainBody.toString() :
      htmlBody.toString().replaceAll("<[^>]*>", " ");
  }

  List<InvoiceFile> attachments() {
    return List.copyOf(attachments);
  }

  private void walkParts(Multipart multipart, int depth)
    throws MessagingException, IOException {
    for (var index = 0; index < multipart.getCount(); index++) {
      walk(multipart.getBodyPart(index), depth);
    }
  }

  private void collect(Part part) throws MessagingException, IOException {
    var fileName = fileName(part);
    if (fileName != null ||
      Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
      attach(part, fileName == null ? "" : fileName);
    } else if (part.isMimeType("text/plain")) {
      append(plainBody, part);
    } else if (part.isMimeType("text/html")) {
      append(htmlBody, part);
    }
  }

  private void attach(Part part, String fileName)
    throws MessagingException, IOException {
    if (attachments.size() >= MAXIMUM_ATTACHMENTS ||
      !mayBeInvoice(part, fileName)) {
      return;
    }
    try (var stream = part.getInputStream()) {
      var content = stream.readNBytes(MAXIMUM_ATTACHMENT_BYTES + 1);
      if (content.length <= MAXIMUM_ATTACHMENT_BYTES) {
        attachments.add(new InvoiceFile(fileName, part.getContentType(),
          content));
      }
    }
  }

  /**
   * Inline images are logos and signatures, not scanned invoices.
   */
  private boolean mayBeInvoice(Part part, String fileName)
    throws MessagingException {
    var file = new InvoiceFile(fileName, part.getContentType(), new byte[0]);
    var inline = Part.INLINE.equalsIgnoreCase(part.getDisposition());
    return file.isPdf() || file.isXml() || (file.isImage() && !inline);
  }

  private void append(StringBuilder body, Part part)
    throws MessagingException, IOException {
    if (body.length() < MAXIMUM_BODY_CHARACTERS &&
      part.getContent() instanceof String text) {
      body.append(text, 0, Math.min(text.length(),
        MAXIMUM_BODY_CHARACTERS - body.length())).append('\n');
    }
  }

  private String fileName(Part part) throws MessagingException {
    var name = part.getFileName();
    if (name == null) {
      return null;
    }
    try {
      return MimeUtility.decodeText(name);
    } catch (IOException exception) {
      return name;
    }
  }
}