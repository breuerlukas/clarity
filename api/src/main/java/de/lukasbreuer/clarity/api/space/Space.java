package de.lukasbreuer.clarity.api.space;

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
 * A separate set of bank connections (and, for a business, contracts) with
 * its own name and icon, such as the owner's own money, a shared household
 * or a company. The slug names it in the panel's addresses. The first space
 * is created on its own and has no name until the owner gives it one; the
 * panel shows it as "Personal" in the owner's language.
 */
@Entity
@Table(name = "space")
@Getter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(staticName = "create")
public final class Space {
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;
  @Column(name = "slug", nullable = false, unique = true)
  private String slug;
  @Column(name = "name", nullable = false)
  private String name;
  @Column(name = "icon", nullable = false)
  private String icon;
  @Enumerated(EnumType.STRING)
  @Column(name = "kind", nullable = false, updatable = false)
  private SpaceKind kind;
  @Column(name = "created_at", nullable = false, updatable = false)
  private long createdAt;

  public boolean isBusiness() {
    return kind == SpaceKind.BUSINESS;
  }

  public void change(String name, String slug, String icon) {
    this.name = name;
    this.slug = slug;
    this.icon = icon;
  }

  public Map<String, Object> information() {
    return Map.of("id", id,
      "slug", slug,
      "name", name,
      "icon", icon,
      "kind", kind.name());
  }
}
