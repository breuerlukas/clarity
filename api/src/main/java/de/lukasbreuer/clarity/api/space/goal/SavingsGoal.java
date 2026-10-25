package de.lukasbreuer.clarity.api.space.goal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
 * Something the owner saves up for in a space. The money is either set
 * aside by hand, still lying on the everyday accounts, or the balance of a
 * linked savings account. The panel works out progress and how much is
 * needed per month, and takes the money set aside out of the spending room.
 */
@Entity
@Table(name = "savings_goal")
@Getter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(staticName = "create")
public final class SavingsGoal {
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;
  @Column(name = "space_id", nullable = false, updatable = false)
  private UUID spaceId;
  @Column(name = "name", nullable = false)
  private String name;
  @Column(name = "icon", nullable = false)
  private String icon;
  @Column(name = "target", nullable = false, precision = 19, scale = 2)
  private BigDecimal target;
  @Column(name = "target_date")
  private LocalDate targetDate;
  @Column(name = "account_id")
  private UUID accountId;
  @Column(name = "saved", nullable = false, precision = 19, scale = 2)
  private BigDecimal saved;
  @Column(name = "created_at", nullable = false, updatable = false)
  private long createdAt;

  public void edit(
    String name, String icon, BigDecimal target, LocalDate targetDate,
    UUID accountId
  ) {
    this.name = name;
    this.icon = icon;
    this.target = target;
    this.targetDate = targetDate;
    this.accountId = accountId;
  }

  /**
   * Taking out more than was set aside is refused by the caller.
   */
  public boolean allowsDeposit(BigDecimal amount) {
    return saved.add(amount).signum() >= 0;
  }

  public void deposit(BigDecimal amount) {
    saved = saved.add(amount);
  }

  public Map<String, Object> information() {
    var information = new HashMap<String, Object>();
    information.put("id", id.toString());
    information.put("space", spaceId.toString());
    information.put("name", name);
    information.put("icon", icon);
    information.put("target", target);
    information.put("target_date", targetDate == null ? null :
      targetDate.toString());
    information.put("account", accountId == null ? null :
      accountId.toString());
    information.put("saved", saved);
    information.put("created_at", createdAt);
    return information;
  }
}
