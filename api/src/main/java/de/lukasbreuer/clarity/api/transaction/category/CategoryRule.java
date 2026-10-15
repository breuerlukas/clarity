package de.lukasbreuer.clarity.api.transaction.category;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Locale;

/**
 * "Everything from this counterparty belongs to that category", created when
 * the owner re-categorizes a transaction and asks to remember it. The keyword
 * is the normalized counterparty name and doubles as the id, so remembering
 * the same counterparty again replaces the old rule.
 */
@Entity
@Table(name = "category_rule")
@Getter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(staticName = "create")
public final class CategoryRule {
  public static CategoryRule forCounterparty(
    String counterparty, String category
  ) {
    var keyword = counterparty.trim().replaceAll("\\s+", " ")
      .toLowerCase(Locale.ROOT);
    return create(keyword, category);
  }

  @Id
  @Column(name = "keyword", nullable = false, updatable = false)
  private String keyword;
  @Column(name = "category", nullable = false)
  private String category;
}