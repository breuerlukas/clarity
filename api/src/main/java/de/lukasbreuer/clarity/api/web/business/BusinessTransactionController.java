package de.lukasbreuer.clarity.api.web.business;

import de.lukasbreuer.clarity.api.banking.account.AccountScope;
import de.lukasbreuer.clarity.api.business.contract.ContractRepository;
import de.lukasbreuer.clarity.api.business.invoice.Invoice;
import de.lukasbreuer.clarity.api.business.invoice.InvoicePayment;
import de.lukasbreuer.clarity.api.business.invoice.InvoicePaymentRepository;
import de.lukasbreuer.clarity.api.business.invoice.InvoiceRepository;
import de.lukasbreuer.clarity.api.business.payment.PaymentAssignment;
import de.lukasbreuer.clarity.api.business.payment.PaymentAssignmentRepository;
import de.lukasbreuer.clarity.api.business.payment.PaymentKind;
import de.lukasbreuer.clarity.api.space.SpaceKind;
import de.lukasbreuer.clarity.api.transaction.Transaction;
import de.lukasbreuer.clarity.api.transaction.TransactionRepository;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;
import de.lukasbreuer.clarity.api.web.request.UuidParameter;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import de.lukasbreuer.clarity.api.web.transaction.TransactionController.DateRange;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The bookings of a business space, each with what the owner assigned it to
 * by hand. Without such an assignment the panel matches it on its own.
 * Error codes: 3000 invalid date range, 4001 unknown contract, 4003 unknown
 * business transaction, 4004 unknown kind, 5001 invalid space.
 */
@RestController
@RequiredArgsConstructor
public final class BusinessTransactionController {
  private static final long MAXIMUM_RANGE_DAYS = 800;
  private static final String AUTOMATIC = "AUTO";

  private final TransactionRepository transactionRepository;
  private final PaymentAssignmentRepository assignmentRepository;
  private final ContractRepository contractRepository;
  private final InvoiceRepository invoiceRepository;
  private final InvoicePaymentRepository invoicePaymentRepository;
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
      .thenCompose(transactions -> assignmentRepository.findAll()
        .thenApply(assignments -> ApiResponse.success(Map.of("transactions",
          information(transactions, assignments,
            invoices(spaceId.get(), transactions))))));
  }

  /**
   * The invoices each booking pays, by transaction.
   */
  private Map<UUID, List<Map<String, Object>>> invoices(
    UUID spaceId, List<Transaction> transactions
  ) {
    if (transactions.isEmpty()) {
      return Map.of();
    }
    var numbers = invoiceRepository.findBySpaceId(spaceId).join().stream()
      .collect(Collectors.toMap(Invoice::id, Invoice::number));
    return invoicePaymentRepository.findByTransactionIdIn(transactions.stream()
        .map(Transaction::id).toList()).join().stream()
      .filter(payment -> !payment.rejected())
      .filter(payment -> numbers.containsKey(payment.invoiceId()))
      .collect(Collectors.groupingBy(InvoicePayment::transactionId,
        Collectors.mapping(payment -> Map.<String, Object>of(
          "id", payment.invoiceId(),
          "number", numbers.get(payment.invoiceId()),
          "amount", payment.amount()), Collectors.toList())));
  }

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

  private List<Map<String, Object>> information(
    List<Transaction> transactions, List<PaymentAssignment> assignments,
    Map<UUID, List<Map<String, Object>>> invoices
  ) {
    var byTransaction = assignments.stream().collect(Collectors.toMap(
      PaymentAssignment::transactionId, Function.identity()));
    return transactions.stream().map(transaction -> {
      var information = transaction.information();
      var assignment = byTransaction.get(transaction.id());
      information.put("assignment", assignment == null ? null :
        assignment.information());
      information.put("invoices", invoices.getOrDefault(transaction.id(),
        List.of()));
      return information;
    }).toList();
  }
}
