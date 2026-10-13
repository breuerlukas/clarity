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

import java.util.UUID;

/**
 * The original file of an invoice. Kept apart from the invoice so listing
 * invoices never loads the files. The SHA-256 hash recognises a file that
 * arrives a second time, for example in a reply or a forwarded mail.
 */
@Entity
@Table(name = "invoice_document",
  uniqueConstraints = @UniqueConstraint(name = "uq_invoice_document_hash",
    columnNames = {"space_id", "sha256"}))
@Getter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(staticName = "create")
public final class InvoiceDocument {
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;
  @Column(name = "space_id", nullable = false, updatable = false)
  private UUID spaceId;
  @Column(name = "name", nullable = false)
  private String name;
  @Column(name = "content_type", nullable = false)
  private String contentType;
  @Column(name = "sha256", nullable = false, length = 64)
  private String sha256;
  @Column(name = "content", nullable = false)
  private byte[] content;
}
