package de.lukasbreuer.clarity.api.transaction.receipt;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
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
 * A receipt the owner photographed or uploaded, with what was read from it
 * and, once assigned, the transaction it belongs to: a card payment or a
 * cash entry. The file itself lies outside the database, see
 * {@link ReceiptFiles}.
 */
@Entity
@Table(name = "receipt", indexes = {
  @Index(name = "ix_receipt_space", columnList = "space_id"),
  @Index(name = "ix_receipt_transaction", columnList = "transaction_id")})
@Getter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(staticName = "create")
public final class Receipt {
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;
  @Column(name = "space_id", nullable = false, updatable = false)
  private UUID spaceId;
  @Column(name = "transaction_id")
  private UUID transactionId;
  @Column(name = "merchant", nullable = false)
  private String merchant;
  @Column(name = "purchase_date")
  private LocalDate purchaseDate;
  @Column(name = "total", precision = 19, scale = 4)
  private BigDecimal total;
  @Convert(converter = ReceiptItemListConverter.class)
  @Column(name = "items", nullable = false, length = 16000)
  private List<ReceiptItem> items;
  @Column(name = "file_name", nullable = false)
  private String fileName;
  @Column(name = "content_type", nullable = false)
  private String contentType;
  @Column(name = "file_size", nullable = false)
  private long fileSize;
  @Column(name = "created_at", nullable = false, updatable = false)
  private long createdAt;

  /**
   * A cash expense needs both; a card payment brings its own.
   */
  public boolean hasDateAndTotal() {
    return purchaseDate != null && total != null;
  }

  public void edit(
    String merchant, LocalDate purchaseDate, BigDecimal total,
    List<ReceiptItem> items
  ) {
    this.merchant = merchant;
    this.purchaseDate = purchaseDate;
    this.total = total;
    this.items = items;
  }

  public void attachTo(UUID transactionId) {
    this.transactionId = transactionId;
  }

  public Map<String, Object> information() {
    var information = new HashMap<String, Object>();
    information.put("id", id);
    information.put("space", spaceId);
    information.put("transaction", transactionId);
    information.put("merchant", merchant);
    information.put("date", purchaseDate == null ? null :
      purchaseDate.toString());
    information.put("total", total);
    information.put("items", items.stream().map(ReceiptItem::information)
      .toList());
    information.put("file", Map.of("name", fileName,
      "content_type", contentType, "size", fileSize));
    information.put("created_at", createdAt);
    return information;
  }
}