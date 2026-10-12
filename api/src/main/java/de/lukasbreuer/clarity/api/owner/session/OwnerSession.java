package de.lukasbreuer.clarity.api.owner.session;

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

import java.util.UUID;

/**
 * A signed-in browser. Only the SHA-256 hash of the cookie token is stored, so
 * a leaked database does not hand out working sessions.
 */
@Entity
@Table(name = "owner_session")
@Getter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(staticName = "create")
public final class OwnerSession {
  private static final long MAXIMUM_LIFETIME = 1000L * 60 * 60 * 24 * 30;
  private static final long MAXIMUM_IDLE_TIME = 1000L * 60 * 60 * 24 * 7;
  private static final long TOUCH_INTERVAL = 1000L * 60;

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;
  @Column(name = "token_hash", nullable = false, unique = true)
  private String tokenHash;
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private OwnerSessionStatus status;
  @Column(name = "ip_address")
  private String ipAddress;
  @Column(name = "user_agent", length = 512)
  private String userAgent;
  @Column(name = "opened_at", nullable = false, updatable = false)
  private long openedAt;
  @Column(name = "last_seen_at", nullable = false)
  private long lastSeenAt;

  /**
   * A session dies 30 days after login at the latest, or after a week without
   * use, whichever comes first.
   */
  public boolean isValid() {
    var now = System.currentTimeMillis();
    return status.isActive() && now - openedAt < MAXIMUM_LIFETIME &&
      now - lastSeenAt < MAXIMUM_IDLE_TIME;
  }

  /**
   * Whether the last activity is old enough to be worth a database write.
   */
  public boolean needsTouch() {
    return System.currentTimeMillis() - lastSeenAt > TOUCH_INTERVAL;
  }

  public void touch() {
    this.lastSeenAt = System.currentTimeMillis();
  }

  public void close() {
    this.status = OwnerSessionStatus.CLOSED;
  }
}