package de.lukasbreuer.clarity.api.notification;

import de.lukasbreuer.clarity.api.database.DatabaseRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Repository
public interface SentNotificationRepository
  extends DatabaseRepository<SentNotification, UUID> {
  @Async
  CompletableFuture<List<SentNotification>> findByEventKeyIn(
    Collection<String> eventKeys);
}
