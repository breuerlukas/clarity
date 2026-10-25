package de.lukasbreuer.clarity.api.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.UUID;

/**
 * Remembers that a notification went out, so the hourly run sends each
 * event once.
 */
@Entity
@Table(name = "sent_notification")
@Getter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(staticName = "create")
public final class SentNotification {
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;
  @Column(name = "event_key", nullable = false, unique = true,
    updatable = false, length = 600)
  private String eventKey;
  @Column(name = "sent_at", nullable = false, updatable = false)
  private long sentAt;
}
