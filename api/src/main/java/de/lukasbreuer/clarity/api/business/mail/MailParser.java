package de.lukasbreuer.clarity.api.business.mail;

import de.lukasbreuer.clarity.api.business.invoice.reading.InvoiceFile;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeUtility;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Takes a mail apart into its text and the attachments that may be
 * invoices: PDFs, XML files and attached (not inline) images, each at most
 * ten megabytes. Mails forwarded as attachments are opened too, a few
 * levels deep.
 */
public final class MailParser {
  private static final int MAXIMUM_ATTACHMENT_BYTES = 10 * 1024 * 1024;
  private static final int MAXIMUM_BODY_CHARACTERS = 100_000;
  private static final int MAXIMUM_DEPTH = 5;
  private static final int MAXIMUM_ATTACHMENTS = 20;

  private final StringBuilder plainBody = new StringBuilder();
  private final StringBuilder htmlBody = new StringBuilder();
  private final List<InvoiceFile> attachments = new ArrayList<>();

  private MailParser() {
  }

  public static ReceivedMail parse(Message message)
    throws MessagingException, IOException {
    var parser = new MailParser();
    parser.walk(message, 0);
    var received = message.getReceivedDate() != null ?
      message.getReceivedDate() : message.getSentDate();
    var body = !parser.plainBody.isEmpty() ? parser.plainBody.toString() :
      parser.htmlBody.toString().replaceAll("<[^>]*>", " ");
    return new ReceivedMail(
      message.getSubject() == null ? "" : message.getSubject(),
      name(message.getFrom()),
      name(message.getRecipients(Message.RecipientType.TO)),
      received == null ? System.currentTimeMillis() : received.getTime(),
      body, List.copyOf(parser.attachments));
  }

  private static String name(Address[] addresses) {
    if (addresses == null || addresses.length == 0) {
      return "";
    }
    if (addresses[0] instanceof InternetAddress address) {
      return address.getPersonal() != null && !address.getPersonal().isBlank() ?
        address.getPersonal() : address.getAddress();
    }
    return addresses[0].toString();
  }

  private void walk(Part part, int depth)
    throws MessagingException, IOException {
    if (depth > MAXIMUM_DEPTH) {
      return;
    }
    if (part.isMimeType("multipart/*") &&
      part.getContent() instanceof Multipart multipart) {
      for (var index = 0; index < multipart.getCount(); index++) {
        walk(multipart.getBodyPart(index), depth + 1);
      }
      return;
    }
    if (part.isMimeType("message/rfc822") &&
      part.getContent() instanceof Part nested) {
      walk(nested, depth + 1);
      return;
    }
    var fileName = fileName(part);
    if (fileName != null || Part.ATTACHMENT.equalsIgnoreCase(
      part.getDisposition())) {
      attach(part, fileName == null ? "" : fileName);
      return;
    }
    if (part.isMimeType("text/plain")) {
      append(plainBody, part);
    } else if (part.isMimeType("text/html")) {
      append(htmlBody, part);
    }
  }

  private void attach(Part part, String fileName)
    throws MessagingException, IOException {
    var file = new InvoiceFile(fileName, part.getContentType(), new byte[0]);
    var inline = Part.INLINE.equalsIgnoreCase(part.getDisposition());
    if (attachments.size() >= MAXIMUM_ATTACHMENTS ||
      !(file.isPdf() || file.isXml() || (file.isImage() && !inline))) {
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

  private void append(StringBuilder body, Part part)
    throws MessagingException, IOException {
    if (body.length() < MAXIMUM_BODY_CHARACTERS &&
      part.getContent() instanceof String text) {
      body.append(text, 0, Math.min(text.length(),
        MAXIMUM_BODY_CHARACTERS - body.length())).append('\n');
    }
  }

  private static String fileName(Part part) throws MessagingException {
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
