package de.lukasbreuer.clarity.api.space;

import de.lukasbreuer.clarity.api.database.DatabaseRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Repository
public interface SpaceRepository extends DatabaseRepository<Space, UUID> {
  @Async
  CompletableFuture<List<Space>> findByKind(SpaceKind kind);
}
