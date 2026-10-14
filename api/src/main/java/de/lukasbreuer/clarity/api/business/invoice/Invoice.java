package de.lukasbreuer.clarity.api.business.invoice;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * An invoice of a business space, read from a mail. The gross amount is what
 * is to be paid. Settled means the owner (or an early-payment discount)
 * closed it although less than the gross amount arrived. The original file
 * is kept as an {@link InvoiceDocument}; the mail it came with is described
 * by subject, sender and arrival.
 */
@Entity
@Table(name = "invoice",
  indexes = @Index(name = "ix_invoice_space", columnList = "space_id"))
@Getter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(staticName = "create")
public final class Invoice {
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;
  @Column(name = "space_id", nullable = false, updatable = false)
  private UUID spaceId;
  @Enumerated(EnumType.STRING)
  @Column(name = "direction", nullable = false)
  private InvoiceDirection direction;
  @Column(name = "number", nullable = false)
  private String number;
  @Column(name = "issuer", nullable = false)
  private String issuer;
  @Column(name = "recipient", nullable = false)
  private String recipient;
  @Column(name = "iban")
  private String iban;
  @Column(name = "currency", nullable = false)
  private String currency;
  @Column(name = "net_amount", precision = 19, scale = 2)
  private BigDecimal netAmount;
  @Column(name = "tax_amount", precision = 19, scale = 2)
  private BigDecimal taxAmount;
  @Column(name = "gross_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal grossAmount;
  @Column(name = "invoice_date", nullable = false)
  private LocalDate invoiceDate;
  @Column(name = "due_date")
  private LocalDate dueDate;
  @Enumerated(EnumType.STRING)
  @Column(name = "format", nullable = false, updatable = false)
  private InvoiceFormat format;
  @Column(name = "needs_review", nullable = false)
  private boolean needsReview;
  @Column(name = "settled", nullable = false)
  private boolean settled;
  @Column(name = "announced_at")
  private LocalDate announcedAt;
  @Column(name = "announced_amount", precision = 19, scale = 2)
  private BigDecimal announcedAmount;
  @Column(name = "mailbox_id")
  private UUID mailboxId;
  @Column(name = "mail_subject", length = 500)
  private String mailSubject;
  @Column(name = "mail_sender", length = 500)
  private String mailSender;
  @Column(name = "mail_received_at")
  private Long mailReceivedAt;
  @Column(name = "document_id")
  private UUID documentId;
  @Column(name = "document_name")
  private String documentName;
  @Column(name = "document_content_type")
  private String documentContentType;
  @Column(name = "document_size")
  private Long documentSize;
  @Column(name = "created_at", nullable = false, updatable = false)
  private long createdAt;

  /** The owner's corrections; they count as reviewed. */
  public void edit(InvoiceDirection direction, InvoiceFields fields) {
    this.direction = direction;
    editDetails(fields);
    editAmounts(fields);
    needsReview = false;
  }

  private void editDetails(InvoiceFields fields) {
    number = fields.number();
    issuer = fields.issuer();
    recipient = fields.recipient();
    iban = fields.iban();
    invoiceDate = fields.invoiceDate();
    dueDate = fields.dueDate();
  }

  private void editAmounts(InvoiceFields fields) {
    currency = fields.currency();
    netAmount = fields.netAmount();
    taxAmount = fields.taxAmount();
    grossAmount = fields.grossAmount();
  }

  public void settle(boolean settled) {
    this.settled = settled;
  }

  public void announce(LocalDate date, BigDecimal amount) {
    announcedAt = date;
    announcedAmount = amount;
  }

  public BigDecimal openAmount(List<InvoicePayment> payments) {
    return InvoiceBalance.of(this, payments).open();
  }

  public boolean isPaid(List<InvoicePayment> payments) {
    return InvoiceBalance.of(this, payments).isPaid();
  }

  public InvoiceStatus status(List<InvoicePayment> payments, LocalDate today) {
    return InvoiceBalance.of(this, payments).status(today);
  }

  /**
   * @param payments The payments of this invoice; rejected ones are left out
   */
  public Map<String, Object> information(
    List<InvoicePayment> payments, LocalDate today
  ) {
    return InvoiceInformation.of(this, InvoiceBalance.of(this, payments),
      today).toMap();
  }
}