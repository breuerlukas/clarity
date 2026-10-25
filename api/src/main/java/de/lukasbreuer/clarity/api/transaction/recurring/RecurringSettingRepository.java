package de.lukasbreuer.clarity.api.transaction.recurring;

import de.lukasbreuer.clarity.api.database.DatabaseRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Repository
public interface RecurringSettingRepository
  extends DatabaseRepository<RecurringSetting, UUID> {
  @Async
  CompletableFuture<List<RecurringSetting>> findBySpaceId(UUID spaceId);

  @Async
  CompletableFuture<Optional<RecurringSetting>> findBySpaceIdAndPaymentKey(
    UUID spaceId, String paymentKey);
}
