package de.lukasbreuer.clarity.api.business.mail;

import de.lukasbreuer.clarity.api.database.DatabaseRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Repository
public interface MailFolderCursorRepository
  extends DatabaseRepository<MailFolderCursor, UUID> {
  @Async
  CompletableFuture<Optional<MailFolderCursor>> findByMailboxIdAndFolder(
    UUID mailboxId, String folder);

  /**
   * Synchronous on purpose: Spring Data only allows void or a row count as the
   * result of a modifying query.
   */
  @Modifying
  @Transactional
  @Query("delete from MailFolderCursor entry " +
    "where entry.mailboxId = :mailboxId")
  void deleteByMailboxId(@Param("mailboxId") UUID mailboxId);
}