package de.lukasbreuer.clarity.api.space.goal;

import de.lukasbreuer.clarity.api.database.DatabaseRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Repository
public interface SavingsGoalRepository
  extends DatabaseRepository<SavingsGoal, UUID> {
  @Async
  CompletableFuture<List<SavingsGoal>> findBySpaceIdOrderByCreatedAt(
    UUID spaceId);

  @Async
  CompletableFuture<Boolean> existsBySpaceId(UUID spaceId);
}
