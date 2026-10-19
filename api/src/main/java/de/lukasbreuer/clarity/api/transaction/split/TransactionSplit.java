package de.lukasbreuer.clarity.api.transaction.split;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * One part of a transaction that spans several categories, such as the
 * drugstore items on a supermarket receipt. The parts of a transaction add
 * up to its amount and carry its sign; the transaction keeps its own
 * category for lists, while statistics count the parts.
 */
@Entity
@Table(name = "transaction_split",
  indexes = @Index(name = "ix_transaction_split_transaction",
    columnList = "transaction_id"))
@Getter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(staticName = "create")
public final class TransactionSplit {
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;
  @Column(name = "transaction_id", nullable = false, updatable = false)
  private UUID transactionId;
  @Column(name = "category", nullable = false)
  private String category;
  @Column(name = "amount", nullable = false, precision = 19, scale = 4)
  private BigDecimal amount;

  public Map<String, Object> information() {
    return Map.of("category", category, "amount", amount);
  }
}