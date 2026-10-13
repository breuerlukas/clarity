package de.lukasbreuer.clarity.api.business.invoice;

import de.lukasbreuer.clarity.api.database.DatabaseRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Repository
public interface InvoiceRepository extends DatabaseRepository<Invoice, UUID> {
  @Async
  CompletableFuture<List<Invoice>> findBySpaceId(UUID spaceId);

  @Async
  CompletableFuture<Boolean> existsBySpaceId(UUID spaceId);
}
