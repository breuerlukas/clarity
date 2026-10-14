package de.lukasbreuer.clarity.api.web.business;

import de.lukasbreuer.clarity.api.banking.account.AccountScope;
import de.lukasbreuer.clarity.api.business.contract.ContractRepository;
import de.lukasbreuer.clarity.api.business.payment.PaymentAssignment;
import de.lukasbreuer.clarity.api.business.payment.PaymentAssignmentRepository;
import de.lukasbreuer.clarity.api.business.payment.PaymentKind;
import de.lukasbreuer.clarity.api.space.SpaceKind;
import de.lukasbreuer.clarity.api.transaction.Transaction;
import de.lukasbreuer.clarity.api.transaction.TransactionRepository;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Assigns a booking of a business space by hand: to a contract, as another
 * kind of payment, or back to the automatic match. Error codes: 4001
 * unknown contract, 4003 unknown business transaction, 4004 unknown kind.
 */
@RestController
@RequiredArgsConstructor
public final class PaymentAssignmentController {
  private static final String AUTOMATIC = "AUTO";

  private final TransactionRepository transactionRepository;
  private final PaymentAssignmentRepository assignmentRepository;
  private final ContractRepository contractRepository;
  private final AccountScope accountScope;

  /**
   * Sets what a booking is. The kind "AUTO" forgets the owner's choice and
   * leaves it to the automatic match again.
   */
  @RequestMapping(path = "/business/transactions/assign/",
    method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> assign(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var transactionId = body.getUUID("id");
    var kindName = body.getString("kind", 16);
    var kind = PaymentKind.find(kindName);
    if (transactionId == null) {
      return ApiResponse.error(4003).future();
    }
    if (kind.isEmpty() && !kindName.equals(AUTOMATIC)) {
      return ApiResponse.error(4004).future();
    }
    var contractId = kind.filter(PaymentKind.CONTRACT::equals).isPresent() ?
      body.getUUID("contract") : null;
    return findBusinessTransaction(transactionId)
      .thenCompose(transaction -> transaction.isEmpty() ?
        ApiResponse.error(4003).future() :
        assign(transaction.get(), kind, contractId));
  }

  private CompletableFuture<Optional<Transaction>> findBusinessTransaction(
    UUID transactionId
  ) {
    return accountScope.accountIds(SpaceKind.BUSINESS)
      .thenCompose(accountIds -> transactionRepository.findById(transactionId)
        .thenApply(transaction -> transaction.filter(entry ->
          accountIds.contains(entry.accountId()))));
  }

  private CompletableFuture<ApiResponse> assign(
    Transaction transaction, Optional<PaymentKind> kind, UUID contractId
  ) {
    if (kind.isEmpty()) {
      return assignmentRepository.deleteById(transaction.id())
        .thenApply(_ -> ApiResponse.success());
    }
    var assignment = PaymentAssignment.create(transaction.id(),
      transaction.accountId(), kind.get(), contractId);
    if (kind.get() != PaymentKind.CONTRACT) {
      return save(assignment);
    }
    if (contractId == null) {
      return ApiResponse.error(4001).future();
    }
    return contractRepository.existsById(contractId)
      .thenCompose(exists -> exists ? save(assignment) :
        ApiResponse.error(4001).future());
  }

  private CompletableFuture<ApiResponse> save(PaymentAssignment assignment) {
    return assignmentRepository.save(assignment)
      .thenApply(_ -> ApiResponse.success());
  }
}