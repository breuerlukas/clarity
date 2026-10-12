package de.lukasbreuer.clarity.api.owner.session;

import de.lukasbreuer.clarity.api.database.DatabaseRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Repository
public interface OwnerSessionRepository
  extends DatabaseRepository<OwnerSession, UUID> {
  @Async
  CompletableFuture<Optional<OwnerSession>> findByTokenHash(String tokenHash);

  @Async
  CompletableFuture<List<OwnerSession>> findByStatus(OwnerSessionStatus status);
}