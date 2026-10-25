package de.lukasbreuer.clarity.api.transaction.budget;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * How much the owner wants to spend on one category per pay cycle in one
 * space. Categories are shared by every space, budgets are not: a household
 * plans its groceries apart from the personal ones. The panel works out
 * what was spent, the same way it works out the spending room.
 */
@Entity
@Table(name = "category_budget",
  uniqueConstraints = @UniqueConstraint(name = "uq_category_budget",
    columnNames = {"space_id", "category"}))
@Getter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(staticName = "create")
public final class CategoryBudget {
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;
  @Column(name = "space_id", nullable = false, updatable = false)
  private UUID spaceId;
  @Column(name = "category", nullable = false, updatable = false)
  private String category;
  @Column(name = "monthly_limit", nullable = false, precision = 19,
    scale = 4)
  private BigDecimal limit;

  public void changeLimit(BigDecimal limit) {
    this.limit = limit;
  }

  public Map<String, Object> information() {
    return Map.of("category", category, "limit", limit);
  }
}