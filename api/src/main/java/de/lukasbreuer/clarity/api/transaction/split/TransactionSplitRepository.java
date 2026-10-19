package de.lukasbreuer.clarity.api.transaction.split;

import de.lukasbreuer.clarity.api.database.DatabaseRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Repository
public interface TransactionSplitRepository
  extends DatabaseRepository<TransactionSplit, UUID> {
  @Async
  CompletableFuture<List<TransactionSplit>> findByTransactionIdIn(
    Collection<UUID> transactionIds);

  /**
   * No transactions means no parts instead of a query with an empty list.
   */
  default CompletableFuture<List<TransactionSplit>> findOfTransactions(
    Collection<UUID> transactionIds
  ) {
    if (transactionIds.isEmpty()) {
      return CompletableFuture.completedFuture(List.of());
    }
    return findByTransactionIdIn(transactionIds);
  }

  /**
   * Synchronous on purpose: Spring Data only allows void or a row count as
   * the result of a modifying query.
   */
  @Modifying
  @Transactional
  @Query("delete from TransactionSplit split " +
    "where split.transactionId = :transactionId")
  void deleteByTransactionId(@Param("transactionId") UUID transactionId);

  /**
   * Synchronous for the same reason; goes before the transactions of a
   * removed account.
   */
  @Modifying
  @Transactional
  @Query("delete from TransactionSplit split where split.transactionId in " +
    "(select transaction.id from Transaction transaction " +
    "where transaction.accountId = :accountId)")
  void deleteByAccountId(@Param("accountId") UUID accountId);

  /**
   * Synchronous for the same reason; follows a deleted category.
   */
  @Modifying
  @Transactional
  @Query("update TransactionSplit split " +
    "set split.category = :target where split.category = :source")
  int moveCategory(
    @Param("source") String source, @Param("target") String target);
}