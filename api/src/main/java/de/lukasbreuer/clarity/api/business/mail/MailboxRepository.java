package de.lukasbreuer.clarity.api.business.mail;

import de.lukasbreuer.clarity.api.database.DatabaseRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Repository
public interface MailboxRepository extends DatabaseRepository<Mailbox, UUID> {
  @Async
  CompletableFuture<List<Mailbox>> findBySpaceId(UUID spaceId);

  @Async
  CompletableFuture<Boolean> existsBySpaceId(UUID spaceId);
}