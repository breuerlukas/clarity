package de.lukasbreuer.clarity.api.web.transaction;

import de.lukasbreuer.clarity.api.banking.account.AccountScope;
import de.lukasbreuer.clarity.api.transaction.TransactionRepository;
import de.lukasbreuer.clarity.api.transaction.recurring.RecurringPayment;
import de.lukasbreuer.clarity.api.transaction.recurring.RecurringPaymentDetector;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * The personal fixed costs found in the last 13 months, enough history to
 * catch yearly payments as well.
 */
@RestController
@RequiredArgsConstructor
public final class RecurringPaymentController {
  private static final int HISTORY_DAYS = 400;

  private final TransactionRepository transactionRepository;
  private final AccountScope accountScope;

  @RequestMapping(path = "/recurring/", method = RequestMethod.GET)
  public CompletableFuture<ApiResponse> findRecurringPayments() {
    var today = LocalDate.now();
    return accountScope.accountIds(false)
      .thenCompose(accountIds -> transactionRepository.findByAccountsBetween(
        accountIds, today.minusDays(HISTORY_DAYS), today))
      .thenApply(transactions -> RecurringPaymentDetector.create(today)
        .detect(transactions))
      .thenApply(payments -> ApiResponse.success(Map.of("payments",
        payments.stream().map(RecurringPayment::information).toList())));
  }
}