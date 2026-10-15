package de.lukasbreuer.clarity.api.transaction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * One booked transaction. The amount is signed: money out is negative. The
 * external id is the bank's own reference, unique per account, so a sync that
 * sees the same transaction twice stores it once.
 */
@Entity
@Table(name = "transactions",
  uniqueConstraints = @UniqueConstraint(name = "uq_transaction_reference",
    columnNames = {"account_id", "external_id"}),
  indexes = @Index(name = "ix_transaction_booking_date",
    columnList = "booking_date"))
@Getter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(staticName = "create")
public final class Transaction {
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;
  @Column(name = "account_id", nullable = false)
  private UUID accountId;
  @Column(name = "external_id", nullable = false)
  private String externalId;
  @Column(name = "booking_date", nullable = false)
  private LocalDate bookingDate;
  @Column(name = "amount", nullable = false, precision = 19, scale = 4)
  private BigDecimal amount;
  @Column(name = "currency", nullable = false)
  private String currency;
  @Column(name = "counterparty", nullable = false)
  private String counterparty;
  @Column(name = "counterparty_iban")
  private String counterpartyIban;
  @Column(name = "description", nullable = false, length = 1000)
  private String description;
  @Column(name = "category", nullable = false)
  private String category;
  @Column(name = "manually_categorized", nullable = false)
  private boolean manuallyCategorized;

  public boolean isExpense() {
    return amount.signum() < 0;
  }

  public void categorize(String category) {
    this.category = category;
  }

  /**
   * A category the owner picked by hand; rules never overwrite it again.
   */
  public void categorizeManually(String category) {
    this.category = category;
    this.manuallyCategorized = true;
  }

  public Map<String, Object> information() {
    var information = new HashMap<String, Object>();
    information.put("id", id);
    information.put("account", accountId);
    information.put("date", bookingDate.toString());
    information.put("amount", amount);
    information.put("currency", currency);
    information.put("counterparty", counterparty);
    information.put("counterparty_iban", counterpartyIban);
    information.put("description", description);
    information.put("category", category);
    return information;
  }
}