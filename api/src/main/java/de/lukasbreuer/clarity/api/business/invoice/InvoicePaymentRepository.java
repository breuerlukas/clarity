package de.lukasbreuer.clarity.api.business.invoice;

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
public interface InvoicePaymentRepository
  extends DatabaseRepository<InvoicePayment, UUID> {
  @Async
  CompletableFuture<List<InvoicePayment>> findByInvoiceIdIn(
    Collection<UUID> invoiceIds);

  @Async
  CompletableFuture<List<InvoicePayment>> findByTransactionIdIn(
    Collection<UUID> transactionIds);

  /**
   * Synchronous on purpose: Spring Data only allows void or a row count as the
   * result of a modifying query.
   */
  @Modifying
  @Transactional
  @Query("delete from InvoicePayment payment " +
    "where payment.accountId = :accountId")
  void deleteByAccountId(@Param("accountId") UUID accountId);

  @Modifying
  @Transactional
  @Query("delete from InvoicePayment payment " +
    "where payment.invoiceId = :invoiceId")
  void deleteByInvoiceId(@Param("invoiceId") UUID invoiceId);
}