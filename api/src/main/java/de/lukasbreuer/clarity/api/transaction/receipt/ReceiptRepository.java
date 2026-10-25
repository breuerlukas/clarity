package de.lukasbreuer.clarity.api.transaction.receipt;

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
public interface ReceiptRepository extends DatabaseRepository<Receipt, UUID> {
  @Async
  CompletableFuture<List<Receipt>> findBySpaceId(UUID spaceId);

  @Async
  CompletableFuture<Boolean> existsBySpaceId(UUID spaceId);

  @Async
  CompletableFuture<List<Receipt>> findByTransactionIdIn(
    Collection<UUID> transactionIds);

  /**
   * No transactions means no receipts instead of a query with an empty list.
   */
  default CompletableFuture<List<Receipt>> findOfTransactions(
    Collection<UUID> transactionIds
  ) {
    if (transactionIds.isEmpty()) {
      return CompletableFuture.completedFuture(List.of());
    }
    return findByTransactionIdIn(transactionIds);
  }

  /**
   * Synchronous on purpose: Spring Data only allows void or a row count as
   * the result of a modifying query. The receipt stays and can be assigned
   * again.
   */
  @Modifying
  @Transactional
  @Query("update Receipt receipt set receipt.transactionId = null " +
    "where receipt.transactionId = :transactionId")
  void detachFrom(@Param("transactionId") UUID transactionId);

  /**
   * Synchronous for the same reason; goes before the transactions of a
   * removed account.
   */
  @Modifying
  @Transactional
  @Query("update Receipt receipt set receipt.transactionId = null " +
    "where receipt.transactionId in " +
    "(select transaction.id from Transaction transaction " +
    "where transaction.accountId = :accountId)")
  void detachFromAccount(@Param("accountId") UUID accountId);
}