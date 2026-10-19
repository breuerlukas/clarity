package de.lukasbreuer.clarity.api.transaction.cash;

import de.lukasbreuer.clarity.api.database.DatabaseRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Repository
public interface CashWalletRepository
  extends DatabaseRepository<CashWallet, UUID> {
  @Async
  CompletableFuture<Optional<CashWallet>> findBySpaceId(UUID spaceId);
}