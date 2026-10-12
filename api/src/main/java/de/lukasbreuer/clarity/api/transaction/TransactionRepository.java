package de.lukasbreuer.clarity.api.transaction;

import de.lukasbreuer.clarity.api.database.DatabaseRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Repository
public interface TransactionRepository
  extends DatabaseRepository<Transaction, UUID> {
  @Async
  CompletableFuture<List<Transaction>>
    findByAccountIdAndBookingDateGreaterThanEqual(UUID accountId,
      LocalDate from);

  @Async
  CompletableFuture<List<Transaction>>
    findByBookingDateBetweenOrderByBookingDateDesc(LocalDate from, LocalDate to);

  @Async
  CompletableFuture<List<Transaction>> findByManuallyCategorized(
    boolean manuallyCategorized);

  /**
   * Synchronous on purpose: Spring Data only allows void or a row count as the
   * result of a modifying query.
   */
  @Modifying
  @Transactional
  @Query("delete from Transaction transaction " +
    "where transaction.accountId = :accountId")
  void deleteByAccountId(@Param("accountId") UUID accountId);
}