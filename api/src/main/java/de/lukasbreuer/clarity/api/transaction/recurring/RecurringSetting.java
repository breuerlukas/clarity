package de.lukasbreuer.clarity.api.transaction.recurring;

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

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * What the owner decided about one fixed cost of a space. Fixed costs are
 * derived from the bookings on every request, so these decisions are kept
 * apart, by the same key the detector groups the bookings with.
 */
@Entity
@Table(name = "recurring_setting",
  uniqueConstraints = @UniqueConstraint(name = "uq_recurring_setting",
    columnNames = {"space_id", "payment_key"}))
@Getter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(staticName = "create")
public final class RecurringSetting {
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;
  @Column(name = "space_id", nullable = false, updatable = false)
  private UUID spaceId;
  @Column(name = "payment_key", nullable = false, updatable = false,
    length = 500)
  private String paymentKey;
  /** The day the cancelled subscription ends; null while it runs. */
  @Column(name = "cancelled_on")
  private LocalDate cancelledOn;
  /** The last booking seen when the owner said "not ended". */
  @Column(name = "dismissed_last_date")
  private LocalDate dismissedLastDate;
  @Column(name = "notice_days")
  private Integer noticeDays;
  /**
   * Not a fixed cost at all, only bookings that happen to look like one.
   * Null on rows from before the column existed.
   */
  @Column(name = "ignored")
  private Boolean ignored;

  public void change(
    LocalDate cancelledOn, LocalDate dismissedLastDate, Integer noticeDays,
    boolean ignored
  ) {
    this.cancelledOn = cancelledOn;
    this.dismissedLastDate = dismissedLastDate;
    this.noticeDays = noticeDays;
    this.ignored = ignored;
  }

  public boolean isIgnored() {
    return Boolean.TRUE.equals(ignored);
  }

  public boolean endsBefore(LocalDate nextDate) {
    return cancelledOn != null && !nextDate.isBefore(cancelledOn);
  }

  /**
   * A dismissal only covers the gap it was made for: once a new booking
   * arrives and stops again, the payment can go missing anew.
   */
  public boolean dismisses(LocalDate lastDate) {
    return lastDate.equals(dismissedLastDate);
  }

  public Optional<LocalDate> cancelBy(LocalDate nextDate) {
    return Optional.ofNullable(noticeDays).map(nextDate::minusDays);
  }
}
