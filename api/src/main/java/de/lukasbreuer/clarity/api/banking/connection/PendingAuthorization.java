package de.lukasbreuer.clarity.api.banking.connection;

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
 * A bank login that was started but not finished yet. The random state comes
 * back with the redirect from the bank; only a state issued here, and used
 * within 30 minutes, can complete a connection.
 */
@Entity
@Table(name = "pending_authorization")
@Getter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(staticName = "create")
public final class PendingAuthorization {
  private static final long MAXIMUM_AGE = 1000L * 60 * 30;

  @Id
  @Column(name = "state", nullable = false, updatable = false)
  private String state;
  @Column(name = "bank_name", nullable = false)
  private String bankName;
  @Column(name = "bank_country", nullable = false)
  private String bankCountry;
  @Column(name = "created_at", nullable = false, updatable = false)
  private long createdAt;
  @Column(name = "space_id")
  private UUID spaceId;

  public boolean isExpired() {
    return System.currentTimeMillis() - createdAt > MAXIMUM_AGE;
  }
}