package de.lukasbreuer.clarity.api.business.mail;

import jakarta.mail.Folder;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Opens IMAP connections. The certificate must be valid for the host, and a
 * STARTTLS connection that cannot be upgraded is dropped before the login
 * instead of sending the password in the clear.
 */
@Component
@RequiredArgsConstructor(staticName = "create")
public final class MailServer {
  private static final String CONNECTION_TIMEOUT = "20000";
  private static final String TIMEOUT = "60000";
  private static final int MAXIMUM_FOLDERS = 500;

  public Store connect(
    String host, int port, MailSecurity security, String username,
    String password
  ) throws MessagingException {
    var protocol = security == MailSecurity.SSL ? "imaps" : "imap";
    var store = Session.getInstance(properties(protocol, security))
      .getStore(protocol);
    store.connect(host, port, username, password);
    return store;
  }

  /**
   * The full names of every folder that can hold mails.
   */
  public List<String> folders(Store store) throws MessagingException {
    return Arrays.stream(store.getDefaultFolder().list("*"))
      .filter(this::holdsMessages)
      .map(Folder::getFullName)
      .sorted()
      .limit(MAXIMUM_FOLDERS)
      .toList();
  }

  public boolean exists(Store store, String folder)
    throws MessagingException {
    return store.getFolder(folder).exists();
  }

  private Properties properties(String protocol, MailSecurity security) {
    var prefix = "mail." + protocol + ".";
    var properties = new Properties();
    properties.put("mail.store.protocol", protocol);
    properties.put(prefix + "connectiontimeout", CONNECTION_TIMEOUT);
    properties.put(prefix + "timeout", TIMEOUT);
    properties.put(prefix + "writetimeout", TIMEOUT);
    properties.put(prefix + "ssl.checkserveridentity", "true");
    if (security == MailSecurity.STARTTLS) {
      properties.put(prefix + "starttls.enable", "true");
      properties.put(prefix + "starttls.required", "true");
    }
    return properties;
  }

  private boolean holdsMessages(Folder folder) {
    try {
      return (folder.getType() & Folder.HOLDS_MESSAGES) != 0;
    } catch (MessagingException exception) {
      return false;
    }
  }
}