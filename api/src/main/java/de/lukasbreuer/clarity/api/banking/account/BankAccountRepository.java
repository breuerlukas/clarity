package de.lukasbreuer.clarity.api.banking.account;

import de.lukasbreuer.clarity.api.database.DatabaseRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Repository
public interface BankAccountRepository
  extends DatabaseRepository<BankAccount, UUID> {
  @Async
  CompletableFuture<List<BankAccount>> findByConnectionId(UUID connectionId);
}