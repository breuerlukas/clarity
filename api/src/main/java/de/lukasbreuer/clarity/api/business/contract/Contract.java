package de.lukasbreuer.clarity.api.business.contract;

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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A customer contract of a business space: what the customer pays per
 * invoice, how often, since when, and on which terms it renews. After each
 * term of {@code termMonths} it renews by another term unless it was
 * cancelled at least {@code noticeMonths} before. The end date is the last
 * day the contract runs; without one it is open.
 */
@Entity
@Table(name = "contract")
@Getter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(staticName = "create")
public final class Contract {
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;
  @Column(name = "space_id", updatable = false)
  private UUID spaceId;
  @Column(name = "customer", nullable = false)
  private String customer;
  @Column(name = "amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;
  @Enumerated(EnumType.STRING)
  @Column(name = "billing_interval", nullable = false)
  private BillingInterval billingInterval;
  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;
  @Column(name = "term_months", nullable = false)
  private int termMonths;
  @Column(name = "notice_months", nullable = false)
  private int noticeMonths;
  @Column(name = "end_date")
  private LocalDate endDate;
  @Column(name = "created_at", nullable = false, updatable = false)
  private long createdAt;

  public void edit(
    String customer, BigDecimal amount, BillingInterval billingInterval,
    LocalDate startDate, int termMonths, int noticeMonths
  ) {
    this.customer = customer;
    this.amount = amount;
    this.billingInterval = billingInterval;
    this.startDate = startDate;
    this.termMonths = termMonths;
    this.noticeMonths = noticeMonths;
  }

  /**
   * @param endDate The last day the contract runs
   */
  public void end(LocalDate endDate) {
    this.endDate = endDate;
  }

  public void resume() {
    this.endDate = null;
  }

  public Map<String, Object> information() {
    var information = new HashMap<String, Object>();
    information.put("id", id);
    information.put("space", spaceId);
    information.put("customer", customer);
    information.put("amount", amount);
    information.put("interval", billingInterval.name());
    information.put("start_date", startDate.toString());
    information.put("term_months", termMonths);
    information.put("notice_months", noticeMonths);
    information.put("end_date", endDate == null ? null : endDate.toString());
    information.put("created_at", createdAt);
    return information;
  }
}
