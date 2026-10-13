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
import java.util.HashMap;
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
  private static final BigDecimal TOLERANCE = new BigDecimal("0.01");

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

  public InvoiceFields fields() {
    return new InvoiceFields(number, issuer, recipient, iban, currency,
      netAmount, taxAmount, grossAmount, invoiceDate, dueDate);
  }

  /**
   * The owner's corrections; they count as reviewed.
   */
  public void edit(InvoiceDirection direction, InvoiceFields fields) {
    this.direction = direction;
    number = fields.number();
    issuer = fields.issuer();
    recipient = fields.recipient();
    iban = fields.iban();
    currency = fields.currency();
    netAmount = fields.netAmount();
    taxAmount = fields.taxAmount();
    grossAmount = fields.grossAmount();
    invoiceDate = fields.invoiceDate();
    dueDate = fields.dueDate();
    needsReview = false;
  }

  public void settle(boolean settled) {
    this.settled = settled;
  }

  public void announce(LocalDate date, BigDecimal amount) {
    announcedAt = date;
    announcedAmount = amount;
  }

  public BigDecimal paidAmount(List<InvoicePayment> payments) {
    return payments.stream()
      .filter(payment -> payment.invoiceId().equals(id))
      .filter(payment -> !payment.rejected())
      .map(InvoicePayment::amount)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public BigDecimal openAmount(List<InvoicePayment> payments) {
    if (settled) {
      return BigDecimal.ZERO;
    }
    return grossAmount.subtract(paidAmount(payments)).max(BigDecimal.ZERO);
  }

  public boolean isPaid(List<InvoicePayment> payments) {
    return openAmount(payments).compareTo(TOLERANCE) < 0;
  }

  public InvoiceStatus status(List<InvoicePayment> payments, LocalDate today) {
    if (isPaid(payments)) {
      return InvoiceStatus.PAID;
    }
    if (dueDate != null && dueDate.isBefore(today)) {
      return InvoiceStatus.OVERDUE;
    }
    if (paidAmount(payments).signum() > 0) {
      return InvoiceStatus.PARTIAL;
    }
    if (announcedAt != null) {
      return InvoiceStatus.ANNOUNCED;
    }
    return InvoiceStatus.OPEN;
  }

  /**
   * @param payments The payments of this invoice; rejected ones are left out
   */
  public Map<String, Object> information(
    List<InvoicePayment> payments, LocalDate today
  ) {
    var information = new HashMap<String, Object>();
    information.put("id", id);
    information.put("space", spaceId);
    information.put("direction", direction.name());
    information.put("number", number);
    information.put("issuer", issuer);
    information.put("recipient", recipient);
    information.put("iban", iban);
    information.put("currency", currency);
    information.put("net_amount", netAmount);
    information.put("tax_amount", taxAmount);
    information.put("gross_amount", grossAmount);
    information.put("invoice_date", invoiceDate.toString());
    information.put("due_date", dueDate == null ? null : dueDate.toString());
    information.put("format", format.name());
    information.put("needs_review", needsReview);
    information.put("settled", settled);
    information.put("announced_at", announcedAt == null ? null :
      announcedAt.toString());
    information.put("announced_amount", announcedAmount);
    information.put("mail", mailSubject == null ? null : Map.of(
      "subject", mailSubject,
      "sender", mailSender == null ? "" : mailSender,
      "received_at", mailReceivedAt == null ? 0 : mailReceivedAt));
    information.put("document", documentId == null ? null : Map.of(
      "name", documentName,
      "content_type", documentContentType,
      "size", documentSize));
    information.put("payments", payments.stream()
      .filter(payment -> payment.invoiceId().equals(id))
      .filter(payment -> !payment.rejected())
      .map(InvoicePayment::information)
      .toList());
    information.put("paid_amount", paidAmount(payments));
    information.put("open_amount", openAmount(payments));
    information.put("status", status(payments, today).name());
    information.put("created_at", createdAt);
    return information;
  }
}
