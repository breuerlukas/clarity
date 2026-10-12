package de.lukasbreuer.clarity.api.owner;

import de.lukasbreuer.clarity.api.database.DatabaseRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Repository
public interface OwnerRepository extends DatabaseRepository<Owner, UUID> {
  @Async
  CompletableFuture<Optional<Owner>> findFirstByConfirmed(boolean confirmed);

  @Async
  CompletableFuture<Boolean> existsByConfirmed(boolean confirmed);
}