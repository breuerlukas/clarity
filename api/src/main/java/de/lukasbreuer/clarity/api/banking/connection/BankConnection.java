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
 * One consent given at one bank: the Enable Banking session and how long the
 * bank lets it read. A connection can cover several accounts.
 */
@Entity
@Table(name = "bank_connection")
@Getter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(staticName = "create")
public final class BankConnection {
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;
  @Column(name = "session_id", nullable = false)
  private String sessionId;
  @Column(name = "bank_name", nullable = false)
  private String bankName;
  @Column(name = "bank_country", nullable = false)
  private String bankCountry;
  @Column(name = "valid_until", nullable = false)
  private long validUntil;
  @Column(name = "created_at", nullable = false, updatable = false)
  private long createdAt;

  public boolean isValid() {
    return validUntil > System.currentTimeMillis();
  }
}