package de.lukasbreuer.clarity.api.transaction.category;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A category transactions are sorted into. Built-in ones are seeded from
 * {@code default-categories.json} with their former enum name as key, so the
 * keys already stored on transactions and rules stay valid; own ones get a
 * random key. A built-in category without a name shows its translated label.
 * The system categories are the ones the logic depends on (payday,
 * transfers, fixed costs and the categorizer's fallbacks): they can be
 * renamed, but neither hidden nor deleted.
 */
@Entity
@Table(name = "category")
@Getter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(staticName = "create")
public final class Category {
  public static final String SALARY = "SALARY";
  public static final String INCOME = "INCOME";
  public static final String SUBSCRIPTIONS = "SUBSCRIPTIONS";
  public static final String TRANSFER = "TRANSFER";
  public static final String OTHER = "OTHER";
  private static final Set<String> SYSTEM_KEYS =
    Set.of(SALARY, INCOME, SUBSCRIPTIONS, TRANSFER, OTHER);

  @Id
  @Column(name = "category_key", nullable = false, updatable = false)
  private String key;
  @Column(name = "name")
  private String name;
  @Column(name = "icon", nullable = false)
  private String icon;
  @Column(name = "color", nullable = false)
  private String color;
  @Enumerated(EnumType.STRING)
  @Column(name = "kind", nullable = false)
  private CategoryKind kind;
  @Convert(converter = KeywordListConverter.class)
  @Column(name = "keywords", nullable = false, length = 16000)
  private List<String> keywords;
  @Column(name = "hidden", nullable = false)
  private boolean hidden;
  @Column(name = "built_in", nullable = false, updatable = false)
  private boolean builtIn;
  @Column(name = "sort_position", nullable = false)
  private int position;

  public boolean isSystem() {
    return builtIn && SYSTEM_KEYS.contains(key);
  }

  /**
   * System categories keep their kind: payday needs the salary to be income.
   */
  public void edit(
    String name, String icon, String color, CategoryKind kind,
    List<String> keywords
  ) {
    this.name = name;
    this.icon = icon;
    this.color = color;
    this.kind = isSystem() ? this.kind : kind;
    this.keywords = keywords;
  }

  public void hide(boolean hidden) {
    this.hidden = hidden;
  }

  /**
   * @param original The built-in category as it shipped
   */
  public void resetTo(Category original) {
    edit(original.name, original.icon, original.color, original.kind,
      original.keywords);
    this.hidden = false;
  }

  public Map<String, Object> information() {
    var information = new HashMap<String, Object>();
    information.put("key", key);
    information.put("name", name);
    information.put("icon", icon);
    information.put("color", color);
    information.put("kind", kind.name());
    information.put("keywords", keywords);
    information.put("hidden", hidden);
    information.put("built_in", builtIn);
    information.put("system", isSystem());
    return information;
  }
}