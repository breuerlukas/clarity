package de.lukasbreuer.clarity.api.transaction;

import de.lukasbreuer.clarity.api.database.DatabaseRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
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
    findByAccountIdInAndBookingDateBetweenOrderByBookingDateDesc(
      Collection<UUID> accountIds, LocalDate from, LocalDate to);

  /**
   * The transactions of some accounts only, newest first; no accounts means
   * no transactions instead of a query with an empty list.
   */
  default CompletableFuture<List<Transaction>> findByAccountsBetween(
    Collection<UUID> accountIds, LocalDate from, LocalDate to
  ) {
    if (accountIds.isEmpty()) {
      return CompletableFuture.completedFuture(List.of());
    }
    return findByAccountIdInAndBookingDateBetweenOrderByBookingDateDesc(
      accountIds, from, to);
  }

  @Async
  CompletableFuture<List<Transaction>> findByAccountIdIn(
    Collection<UUID> accountIds);

  @Async
  CompletableFuture<Boolean> existsByAccountId(UUID accountId);

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

  /**
   * Synchronous for the same reason. Moves the hand-picked transactions too,
   * since their category is about to be deleted.
   */
  @Modifying
  @Transactional
  @Query("update Transaction transaction " +
    "set transaction.category = :target where transaction.category = :source")
  int moveCategory(
    @Param("source") String source, @Param("target") String target);
}