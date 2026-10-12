package de.lukasbreuer.clarity.api.web.transaction;

import de.lukasbreuer.clarity.api.transaction.Category;
import de.lukasbreuer.clarity.api.transaction.Transaction;
import de.lukasbreuer.clarity.api.transaction.TransactionRepository;
import de.lukasbreuer.clarity.api.transaction.category.CategoryRule;
import de.lukasbreuer.clarity.api.transaction.category.CategoryRuleApplication;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Lists transactions of a date range (at most about two years at once) and
 * changes their category. Error codes: 3000 invalid date range, 3001 unknown
 * transaction, 3002 unknown category.
 */
@RestController
@RequiredArgsConstructor
public final class TransactionController {
  private static final long MAXIMUM_RANGE_DAYS = 800;

  private final TransactionRepository transactionRepository;
  private final CategoryRuleApplication ruleApplication;

  @RequestMapping(path = "/transactions/", method = RequestMethod.GET)
  public CompletableFuture<ApiResponse> findTransactions(
    @RequestParam("from") String from, @RequestParam("to") String to
  ) {
    var range = DateRange.parse(from, to);
    if (range.isEmpty() || range.get().days() > MAXIMUM_RANGE_DAYS) {
      return ApiResponse.error(3000).future();
    }
    return transactionRepository.findByBookingDateBetweenOrderByBookingDateDesc(
        range.get().from(), range.get().to())
      .thenApply(transactions -> ApiResponse.success(Map.of("transactions",
        transactions.stream().map(this::information).toList())));
  }

  /**
   * With {@code remember}, every other transaction from the same counterparty
   * moves along, and future ones land in the category too.
   */
  @RequestMapping(path = "/transactions/category/", method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> categorize(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var transactionId = body.getUUID("id");
    var category = Category.find(body.getString("category", 32));
    if (transactionId == null) {
      return ApiResponse.error(3001).future();
    }
    if (category.isEmpty()) {
      return ApiResponse.error(3002).future();
    }
    var remember = body.getBoolean("remember");
    return transactionRepository.findById(transactionId)
      .thenCompose(transaction -> categorize(transaction, category.get(),
        remember));
  }

  private CompletableFuture<ApiResponse> categorize(
    Optional<Transaction> transaction, Category category, boolean remember
  ) {
    if (transaction.isEmpty()) {
      return ApiResponse.error(3001).future();
    }
    transaction.get().categorizeManually(category);
    var counterparty = transaction.get().counterparty();
    return transactionRepository.save(transaction.get())
      .thenCompose(_ -> remember && !counterparty.isBlank() ?
        ruleApplication.apply(CategoryRule.forCounterparty(counterparty,
          category)) : CompletableFuture.completedFuture(0))
      .thenApply(updated -> ApiResponse.success(Map.of("updated", updated)));
  }

  private Map<String, Object> information(Transaction transaction) {
    var information = new HashMap<String, Object>();
    information.put("id", transaction.id());
    information.put("account", transaction.accountId());
    information.put("date", transaction.bookingDate().toString());
    information.put("amount", transaction.amount());
    information.put("currency", transaction.currency());
    information.put("counterparty", transaction.counterparty());
    information.put("description", transaction.description());
    information.put("category", transaction.category().name());
    return information;
  }

  private record DateRange(LocalDate from, LocalDate to) {
    static Optional<DateRange> parse(String from, String to) {
      try {
        var range = new DateRange(LocalDate.parse(from), LocalDate.parse(to));
        return range.from().isAfter(range.to()) ? Optional.empty() :
          Optional.of(range);
      } catch (Exception exception) {
        return Optional.empty();
      }
    }

    long days() {
      return ChronoUnit.DAYS.between(from, to);
    }
  }
}