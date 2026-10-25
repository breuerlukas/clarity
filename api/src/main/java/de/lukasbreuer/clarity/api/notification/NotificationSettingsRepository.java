package de.lukasbreuer.clarity.api.notification;

import de.lukasbreuer.clarity.api.database.DatabaseRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Repository
public interface NotificationSettingsRepository
  extends DatabaseRepository<NotificationSettings, UUID> {
  @Async
  CompletableFuture<Optional<NotificationSettings>> findFirstByIdNotNull();
}
