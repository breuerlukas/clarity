package de.lukasbreuer.clarity.api.web.transaction;

import de.lukasbreuer.clarity.api.banking.account.AccountScope;
import de.lukasbreuer.clarity.api.transaction.Transaction;
import de.lukasbreuer.clarity.api.transaction.TransactionRepository;
import de.lukasbreuer.clarity.api.transaction.category.CategoryRepository;
import de.lukasbreuer.clarity.api.transaction.category.CategoryRule;
import de.lukasbreuer.clarity.api.transaction.category.CategoryRuleApplication;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;
import de.lukasbreuer.clarity.api.web.request.UuidParameter;
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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Lists the transactions of one space in a date range (at most about two
 * years at once) and changes their category. Error codes: 3000 invalid date
 * range, 3001 unknown transaction, 3002 unknown category, 5001 invalid space.
 */
@RestController
@RequiredArgsConstructor
public final class TransactionController {
  private static final long MAXIMUM_RANGE_DAYS = 800;

  private final TransactionRepository transactionRepository;
  private final CategoryRepository categoryRepository;
  private final CategoryRuleApplication ruleApplication;
  private final AccountScope accountScope;

  @RequestMapping(path = "/transactions/", method = RequestMethod.GET)
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
      .thenApply(transactions -> ApiResponse.success(Map.of("transactions",
        transactions.stream().map(Transaction::information).toList())));
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
    var category = body.getString("category", 64);
    if (transactionId == null) {
      return ApiResponse.error(3001).future();
    }
    var remember = body.getBoolean("remember");
    return categoryRepository.existsById(category)
      .thenCompose(exists -> exists ?
        transactionRepository.findById(transactionId)
          .thenCompose(transaction -> categorize(transaction, category,
            remember)) :
        ApiResponse.error(3002).future());
  }

  private CompletableFuture<ApiResponse> categorize(
    Optional<Transaction> transaction, String category, boolean remember
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

  public record DateRange(LocalDate from, LocalDate to) {
    public static Optional<DateRange> parse(String from, String to) {
      try {
        var range = new DateRange(LocalDate.parse(from), LocalDate.parse(to));
        return range.from().isAfter(range.to()) ? Optional.empty() :
          Optional.of(range);
      } catch (Exception exception) {
        return Optional.empty();
      }
    }

    public long days() {
      return ChronoUnit.DAYS.between(from, to);
    }
  }
}