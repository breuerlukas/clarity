package de.lukasbreuer.clarity.api.notification;

import de.lukasbreuer.clarity.api.business.mail.MailSecurity;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Sends a plain text mail through the owner's SMTP server. As with the
 * mailboxes, the certificate must be valid for the host and STARTTLS is
 * required, so the password never travels in the clear.
 */
@Component
@RequiredArgsConstructor(staticName = "create")
public final class SmtpSender {
  private static final String TIMEOUT = "20000";

  public void send(
    NotificationSettings settings, String password, String subject,
    String text
  ) throws MessagingException {
    var message = new MimeMessage(Session.getInstance(properties(settings)));
    message.setFrom(new InternetAddress(settings.sender(), true));
    message.setRecipient(Message.RecipientType.TO,
      new InternetAddress(settings.recipient(), true));
    message.setSubject(subject, StandardCharsets.UTF_8.name());
    message.setText(text, StandardCharsets.UTF_8.name());
    try (var transport = message.getSession().getTransport()) {
      transport.connect(settings.host(), settings.port(),
        settings.username(), password);
      transport.sendMessage(message, message.getAllRecipients());
    }
  }

  private Properties properties(NotificationSettings settings) {
    var protocol = settings.security() == MailSecurity.SSL ? "smtps" : "smtp";
    var prefix = "mail." + protocol + ".";
    var properties = new Properties();
    properties.put("mail.transport.protocol", protocol);
    properties.put(prefix + "auth", "true");
    properties.put(prefix + "connectiontimeout", TIMEOUT);
    properties.put(prefix + "timeout", TIMEOUT);
    properties.put(prefix + "writetimeout", TIMEOUT);
    properties.put(prefix + "ssl.checkserveridentity", "true");
    if (settings.security() == MailSecurity.STARTTLS) {
      properties.put(prefix + "starttls.enable", "true");
      properties.put(prefix + "starttls.required", "true");
    }
    return properties;
  }
}
