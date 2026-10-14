package de.lukasbreuer.clarity.api.web.business;

import de.lukasbreuer.clarity.api.banking.account.AccountScope;
import de.lukasbreuer.clarity.api.business.payment.PaymentAssignment;
import de.lukasbreuer.clarity.api.business.payment.PaymentAssignmentRepository;
import de.lukasbreuer.clarity.api.transaction.Transaction;
import de.lukasbreuer.clarity.api.transaction.TransactionRepository;
import de.lukasbreuer.clarity.api.web.request.UuidParameter;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import de.lukasbreuer.clarity.api.web.transaction.TransactionController.DateRange;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The bookings of a business space, each with what the owner assigned it to
 * by hand and the invoices it pays. Without such an assignment the panel
 * matches it on its own. Error codes: 3000 invalid date range, 5001 invalid
 * space.
 */
@RestController
@RequiredArgsConstructor
public final class BusinessTransactionController {
  private static final long MAXIMUM_RANGE_DAYS = 800;

  private final TransactionRepository transactionRepository;
  private final PaymentAssignmentRepository assignmentRepository;
  private final PaidInvoices paidInvoices;
  private final AccountScope accountScope;

  @RequestMapping(path = "/business/transactions/", method = RequestMethod.GET)
  public CompletableFuture<ApiResponse> findTransactions(
    @RequestParam("space") String space, @RequestParam("from") String from,
    @RequestParam("to") String to
  ) {
    var spaceId = UuidParameter.parse(space);
    var range = DateRange.parse(from, to);
    if (spaceId.isEmpty()) {
      return ApiResponse.error(5001).future();
    }
    if (range.isEmpty() || range.get().days() > MAXIMUM_RANGE_DAYS) {
      return ApiResponse.error(3000).future();
    }
    return accountScope.accountIds(spaceId.get())
      .thenCompose(accountIds -> transactionRepository.findByAccountsBetween(
        accountIds, range.get().from(), range.get().to()))
      .thenCompose(transactions -> information(spaceId.get(), transactions));
  }

  /**
   * Each booking with its assignment and the invoices it pays.
   */
  private CompletableFuture<ApiResponse> information(
    UUID spaceId, List<Transaction> transactions
  ) {
    return assignmentRepository.findAll()
      .thenCombine(paidInvoices.byTransaction(spaceId, transactions),
        (assignments, invoices) -> ApiResponse.success(Map.of("transactions",
          information(transactions, assignments, invoices))));
  }

  private List<Map<String, Object>> information(
    List<Transaction> transactions, List<PaymentAssignment> assignments,
    Map<UUID, List<Map<String, Object>>> invoices
  ) {
    var byTransaction = assignments.stream().collect(Collectors.toMap(
      PaymentAssignment::transactionId, Function.identity()));
    return transactions.stream()
      .map(transaction -> information(transaction,
        byTransaction.get(transaction.id()),
        invoices.getOrDefault(transaction.id(), List.of())))
      .toList();
  }

  /**
   * @param assignment Null when the automatic match decides
   */
  private Map<String, Object> information(
    Transaction transaction, PaymentAssignment assignment,
    List<Map<String, Object>> invoices
  ) {
    var information = transaction.information();
    information.put("assignment", assignment == null ? null :
      assignment.information());
    information.put("invoices", invoices);
    return information;
  }
}