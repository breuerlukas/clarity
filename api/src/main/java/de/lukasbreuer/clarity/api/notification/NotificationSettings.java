package de.lukasbreuer.clarity.api.notification;

import de.lukasbreuer.clarity.api.business.mail.MailSecurity;
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

import java.util.Map;
import java.util.UUID;

/**
 * Where notifications go and which ones: the SMTP server to send through,
 * with its password encrypted by the SecretBox, the address to send to,
 * the language of the texts and a switch per group of events. There is one
 * row, as there is one owner.
 */
@Entity
@Table(name = "notification_settings")
@Getter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(staticName = "create")
public final class NotificationSettings {
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;
  @Column(name = "enabled", nullable = false)
  private boolean enabled;
  @Column(name = "host", nullable = false)
  private String host;
  @Column(name = "port", nullable = false)
  private int port;
  @Enumerated(EnumType.STRING)
  @Column(name = "security", nullable = false)
  private MailSecurity security;
  @Column(name = "username", nullable = false)
  private String username;
  @Column(name = "password", nullable = false, length = 2000)
  private String encryptedPassword;
  @Column(name = "sender", nullable = false)
  private String sender;
  @Column(name = "recipient", nullable = false)
  private String recipient;
  @Column(name = "language", nullable = false)
  private String language;
  @Column(name = "consent_events", nullable = false)
  private boolean consentEvents;
  @Column(name = "sync_events", nullable = false)
  private boolean syncEvents;
  @Column(name = "price_events", nullable = false)
  private boolean priceEvents;

  /**
   * @param encryptedPassword Null keeps the stored password
   */
  public void edit(NotificationSettings changes, String encryptedPassword) {
    enabled = changes.enabled;
    host = changes.host;
    port = changes.port;
    security = changes.security;
    username = changes.username;
    sender = changes.sender;
    recipient = changes.recipient;
    language = changes.language;
    consentEvents = changes.consentEvents;
    syncEvents = changes.syncEvents;
    priceEvents = changes.priceEvents;
    this.encryptedPassword = encryptedPassword == null ?
      this.encryptedPassword : encryptedPassword;
  }

  public boolean sends(NotificationKind kind) {
    return switch (kind) {
      case CONSENT_EXPIRING, CONSENT_EXPIRED -> consentEvents;
      case SYNC_FAILING -> syncEvents;
      case PRICE_CHANGED -> priceEvents;
      case TEST -> true;
    };
  }

  /**
   * Everything but the password; the panel only learns that one is set.
   */
  public Map<String, Object> information() {
    return Map.ofEntries(Map.entry("enabled", enabled),
      Map.entry("host", host), Map.entry("port", port),
      Map.entry("security", security.name()),
      Map.entry("username", username),
      Map.entry("has_password", !encryptedPassword.isEmpty()),
      Map.entry("sender", sender), Map.entry("recipient", recipient),
      Map.entry("language", language),
      Map.entry("consent_events", consentEvents),
      Map.entry("sync_events", syncEvents),
      Map.entry("price_events", priceEvents));
  }
}
