package de.lukasbreuer.clarity.api.business.mail;

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
 * How far one folder of a mailbox has been read: the highest IMAP UID seen.
 * UIDs only stay valid while the folder's UIDVALIDITY stays the same; when
 * the server changes it, the folder is read again from scratch and the
 * duplicate check of the import keeps invoices from appearing twice.
 */
@Entity
@Table(name = "mail_folder_cursor",
  uniqueConstraints = @UniqueConstraint(name = "uq_mail_folder_cursor",
    columnNames = {"mailbox_id", "folder"}))
@Getter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(staticName = "create")
public final class MailFolderCursor {
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;
  @Column(name = "mailbox_id", nullable = false, updatable = false)
  private UUID mailboxId;
  @Column(name = "folder", nullable = false, updatable = false)
  private String folder;
  @Column(name = "uid_validity", nullable = false)
  private long uidValidity;
  @Column(name = "last_uid", nullable = false)
  private long lastUid;

  public boolean isValidFor(long uidValidity) {
    return this.uidValidity == uidValidity;
  }

  public void advance(long uidValidity, long lastUid) {
    this.uidValidity = uidValidity;
    this.lastUid = lastUid;
  }
}