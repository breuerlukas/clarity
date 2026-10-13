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
  @Test
  void keepsInvoiceAttachmentsAndText() throws Exception {
    var message = new MimeMessage(Session.getInstance(new Properties()));
    message.setFrom(new InternetAddress("billing@weber.example",
      "Druckerei Weber"));
    message.setRecipient(Message.RecipientType.TO,
      new InternetAddress("office@breuer.example"));
    message.setSubject("Ihre Rechnung 2026-1187");
    message.setSentDate(new Date(1_790_000_000_000L));
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
    message.setContent(multipart);
    message.saveChanges();

    var mail = MailParser.parse(message);
    assertEquals("Ihre Rechnung 2026-1187", mail.subject());
    assertEquals("Druckerei Weber", mail.sender());
    assertEquals("office@breuer.example", mail.recipient());
    assertEquals(1_790_000_000_000L, mail.receivedAt());
    assertTrue(mail.body().contains("Anbei Ihre Rechnung."));
    assertEquals(1, mail.attachments().size());
    assertEquals("rechnung.pdf", mail.attachments().getFirst().name());
  }

  @Test
  void recognisesPaymentAdvices() {
    assertTrue(PaymentAdvice.isAdvice(new ReceivedMail("Zahlungsavis 4711",
      "", "", 0, "", List.of())));
    assertTrue(PaymentAdvice.isAdvice(new ReceivedMail("Payment",
      "", "", 0, "Remittance advice for RE-1", List.of())));
    assertFalse(PaymentAdvice.isAdvice(new ReceivedMail("Rechnung 4711",
      "", "", 0, "Anbei die Rechnung", List.of())));
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
