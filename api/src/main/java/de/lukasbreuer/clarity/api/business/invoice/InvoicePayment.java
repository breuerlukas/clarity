package de.lukasbreuer.clarity.api.business.invoice;

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
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The part of a booking that pays an invoice. One transfer may pay several
 * invoices and one invoice may be paid in parts, so the amount is the share
 * of this booking, always positive. A rejected link is one the owner removed
 * after the automatic match made it; it stays so the match does not make it
 * again. The account is kept so removing a bank removes its links too.
 */
@Entity
@Table(name = "invoice_payment",
  uniqueConstraints = @UniqueConstraint(name = "uq_invoice_payment",
    columnNames = {"invoice_id", "transaction_id"}))
@Getter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(staticName = "create")
public final class InvoicePayment {
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;
  @Column(name = "invoice_id", nullable = false, updatable = false)
  private UUID invoiceId;
  @Column(name = "transaction_id", nullable = false, updatable = false)
  private UUID transactionId;
  @Column(name = "account_id", nullable = false, updatable = false)
  private UUID accountId;
  @Column(name = "amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;
  @Column(name = "booking_date", nullable = false)
  private LocalDate bookingDate;
  @Column(name = "automatic", nullable = false)
  private boolean automatic;
  @Column(name = "rejected", nullable = false)
  private boolean rejected;
  @Column(name = "created_at", nullable = false, updatable = false)
  private long createdAt;

  public void reject() {
    rejected = true;
  }

  public Map<String, Object> information() {
    var information = new HashMap<String, Object>();
    information.put("id", id);
    information.put("transaction", transactionId);
    information.put("amount", amount);
    information.put("date", bookingDate.toString());
    information.put("automatic", automatic);
    return information;
  }
}