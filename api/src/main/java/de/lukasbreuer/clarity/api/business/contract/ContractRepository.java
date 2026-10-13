package de.lukasbreuer.clarity.api.business.contract;

import de.lukasbreuer.clarity.api.database.DatabaseRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Repository
public interface ContractRepository
  extends DatabaseRepository<Contract, UUID> {
  @Async
  CompletableFuture<List<Contract>> findBySpaceId(UUID spaceId);

  @Async
  CompletableFuture<Boolean> existsBySpaceId(UUID spaceId);
}
