package de.lukasbreuer.clarity.api.business.invoice;

import de.lukasbreuer.clarity.api.database.DatabaseRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Repository
public interface InvoiceDocumentRepository
  extends DatabaseRepository<InvoiceDocument, UUID> {
  @Async
  CompletableFuture<Boolean> existsBySpaceIdAndSha256(
    UUID spaceId, String sha256);
}
