package de.lukasbreuer.clarity.api.business.mail;

import jakarta.activation.DataHandler;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MailParserTest {
  private final MailParser parser = MailParser.create();

  @Test
  void readsSenderRecipientAndText() throws Exception {
    var mail = parser.parse(invoiceMail());
    assertEquals("Ihre Rechnung 2026-1187", mail.subject());
    assertEquals("Druckerei Weber", mail.sender());
    assertEquals("office@breuer.example", mail.recipient());
    assertEquals(1_790_000_000_000L, mail.receivedAt());
    assertTrue(mail.body().contains("Anbei Ihre Rechnung."));
  }

  @Test
  void keepsOnlyInvoiceAttachments() throws Exception {
    var mail = parser.parse(invoiceMail());
    assertEquals(1, mail.attachments().size());
    assertEquals("rechnung.pdf", mail.attachments().getFirst().name());
  }

  @Test
  void recognisesPaymentAdvices() {
    assertTrue(isAdvice("Zahlungsavis 4711", ""));
    assertTrue(isAdvice("Payment", "Remittance advice for RE-1"));
    assertFalse(isAdvice("Rechnung 4711", "Anbei die Rechnung"));
  }

  private boolean isAdvice(String subject, String body) {
    return PaymentAdvice.of(new ReceivedMail(subject, "", "", 0, body,
      List.of())).isAdvice();
  }

  /**
   * A text, a PDF invoice, an inline logo and an archive.
   */
  private MimeMessage invoiceMail() throws Exception {
    var message = new MimeMessage(Session.getInstance(new Properties()));
    message.setFrom(new InternetAddress("billing@weber.example",
      "Druckerei Weber"));
    message.setRecipient(Message.RecipientType.TO,
      new InternetAddress("office@breuer.example"));
    message.setSubject("Ihre Rechnung 2026-1187");
    message.setSentDate(new Date(1_790_000_000_000L));
    message.setContent(parts());
    message.saveChanges();
    return message;
  }

  private MimeMultipart parts() throws Exception {
    var multipart = new MimeMultipart();
    var text = new MimeBodyPart();
    text.setText("Anbei Ihre Rechnung.", "UTF-8");
    multipart.addBodyPart(text);
    multipart.addBodyPart(attachment("rechnung.pdf", "application/pdf",
      MimeBodyPart.ATTACHMENT));
    multipart.addBodyPart(attachment("logo.png", "image/png",
      MimeBodyPart.INLINE));
    multipart.addBodyPart(attachment("archiv.zip", "application/zip",
      MimeBodyPart.ATTACHMENT));
    return multipart;
  }

  private MimeBodyPart attachment(
    String name, String type, String disposition
  ) throws Exception {
    var part = new MimeBodyPart();
    part.setDataHandler(new DataHandler(new ByteArrayDataSource(
      new byte[] {1, 2, 3}, type)));
    part.setFileName(name);
    part.setDisposition(disposition);
    return part;
  }
}