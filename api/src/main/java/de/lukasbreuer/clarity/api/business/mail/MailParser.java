package de.lukasbreuer.clarity.api.business.mail;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Takes a mail apart into who sent it to whom and when, its text and the
 * attachments that may be invoices (see {@link MailParts}).
 */
@Component
@RequiredArgsConstructor(staticName = "create")
public final class MailParser {
  public ReceivedMail parse(Message message)
    throws MessagingException, IOException {
    var parts = MailParts.create();
    parts.walk(message, 0);
    return new ReceivedMail(subject(message), name(message.getFrom()),
      name(message.getRecipients(Message.RecipientType.TO)),
      receivedAt(message), parts.body(), parts.attachments());
  }

  private String subject(Message message) throws MessagingException {
    return message.getSubject() == null ? "" : message.getSubject();
  }

  /**
   * When the server knows no arrival date, the sender's date is the next
   * best thing, and the moment of reading the last resort.
   */
  private long receivedAt(Message message) throws MessagingException {
    var received = message.getReceivedDate() != null ?
      message.getReceivedDate() : message.getSentDate();
    return received == null ? System.currentTimeMillis() : received.getTime();
  }

  /**
   * The first address's display name, or the address without one.
   */
  private String name(Address[] addresses) {
    if (addresses == null || addresses.length == 0) {
      return "";
    }
    if (addresses[0] instanceof InternetAddress address) {
      return address.getPersonal() != null &&
        !address.getPersonal().isBlank() ? address.getPersonal() :
        address.getAddress();
    }
    return addresses[0].toString();
  }
}