package de.lukasbreuer.clarity.api.business.mail;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * An IMAP mailbox of a business space that invoices arrive in and are sent
 * from. Mails in the incoming folder are invoices to the business and
 * payment advices; mails in the outgoing folder (usually the sent folder)
 * are invoices the business sent. The password is stored encrypted and never
 * leaves the api.
 */
@Entity
@Table(name = "mailbox")
@Getter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(staticName = "create")
public final class Mailbox {
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;
  @Column(name = "space_id", nullable = false, updatable = false)
  private UUID spaceId;
  @Column(name = "host", nullable = false)
  private String host;
  @Column(name = "port", nullable = false)
  private int port;
  @Enumerated(EnumType.STRING)
  @Column(name = "security", nullable = false)
  private MailSecurity security;
  @Column(name = "username", nullable = false)
  private String username;
  @Column(name = "encrypted_password", nullable = false, length = 1000)
  private String encryptedPassword;
  @Column(name = "incoming_folder", nullable = false)
  private String incomingFolder;
  @Column(name = "outgoing_folder")
  private String outgoingFolder;
  @Column(name = "last_checked_at")
  private Long lastCheckedAt;
  @Column(name = "last_error", length = 500)
  private String lastError;
  @Column(name = "created_at", nullable = false, updatable = false)
  private long createdAt;

  /**
   * @param encryptedPassword Null keeps the stored password
   */
  public void edit(
    String host, int port, MailSecurity security, String username,
    String encryptedPassword, String incomingFolder, String outgoingFolder
  ) {
    var serverChanged = !host.equals(this.host) ||
      !username.equals(this.username);
    this.host = host;
    this.port = port;
    this.security = security;
    this.username = username;
    if (encryptedPassword != null) {
      this.encryptedPassword = encryptedPassword;
    }
    this.incomingFolder = incomingFolder;
    this.outgoingFolder = outgoingFolder;
    if (serverChanged) {
      lastError = null;
    }
  }

  public void markChecked() {
    lastCheckedAt = System.currentTimeMillis();
    lastError = null;
  }

  public void markFailed(String error) {
    lastCheckedAt = System.currentTimeMillis();
    lastError = error.substring(0, Math.min(error.length(), 500));
  }

  public Map<String, Object> information() {
    var information = new HashMap<String, Object>();
    information.put("id", id);
    information.put("space", spaceId);
    information.put("host", host);
    information.put("port", port);
    information.put("security", security.name());
    information.put("username", username);
    information.put("incoming_folder", incomingFolder);
    information.put("outgoing_folder", outgoingFolder);
    information.put("last_checked_at", lastCheckedAt);
    information.put("last_error", lastError);
    information.put("created_at", createdAt);
    return information;
  }
}
