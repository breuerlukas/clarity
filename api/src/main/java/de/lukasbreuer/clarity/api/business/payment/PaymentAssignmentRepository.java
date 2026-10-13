package de.lukasbreuer.clarity.api.business.payment;

import de.lukasbreuer.clarity.api.database.DatabaseRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public interface PaymentAssignmentRepository
  extends DatabaseRepository<PaymentAssignment, UUID> {
  /**
   * Synchronous on purpose: Spring Data only allows void or a row count as the
   * result of a modifying query.
   */
  @Modifying
  @Transactional
  @Query("delete from PaymentAssignment assignment " +
    "where assignment.accountId = :accountId")
  void deleteByAccountId(@Param("accountId") UUID accountId);

  @Modifying
  @Transactional
  @Query("delete from PaymentAssignment assignment " +
    "where assignment.contractId = :contractId")
  void deleteByContractId(@Param("contractId") UUID contractId);
}
