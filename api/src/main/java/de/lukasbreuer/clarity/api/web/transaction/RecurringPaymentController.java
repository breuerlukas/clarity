package de.lukasbreuer.clarity.api.web.transaction;

import de.lukasbreuer.clarity.api.banking.account.AccountScope;
import de.lukasbreuer.clarity.api.transaction.TransactionRepository;
import de.lukasbreuer.clarity.api.transaction.recurring.RecurringPayment;
import de.lukasbreuer.clarity.api.transaction.recurring.RecurringPaymentDetector;
import de.lukasbreuer.clarity.api.transaction.recurring.RecurringSetting;
import de.lukasbreuer.clarity.api.transaction.recurring.RecurringSettingRepository;
import de.lukasbreuer.clarity.api.web.request.UuidParameter;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The fixed costs of one space with whether they still run. 26 months of
 * history are enough to see a yearly payment go missing and end. Error
 * code 5001: invalid space.
 */
@RestController
@RequiredArgsConstructor
public final class RecurringPaymentController {
  private static final int HISTORY_DAYS = 800;

  private final TransactionRepository transactionRepository;
  private final AccountScope accountScope;
  private final RecurringSettingRepository settingRepository;

  @RequestMapping(path = "/recurring/", method = RequestMethod.GET)
  public CompletableFuture<ApiResponse> findRecurringPayments(
    @RequestParam("space") String space
  ) {
    var spaceId = UuidParameter.parse(space);
    if (spaceId.isEmpty()) {
      return ApiResponse.error(5001).future();
    }
    var today = LocalDate.now();
    return detect(spaceId.get(), today)
      .thenCombine(settingRepository.findBySpaceId(spaceId.get()),
        (payments, settings) -> paymentsResponse(payments, settings, today));
  }

  private CompletableFuture<List<RecurringPayment>> detect(
    UUID spaceId, LocalDate today
  ) {
    return accountScope.accountIds(spaceId)
      .thenCompose(accountIds -> transactionRepository.findByAccountsBetween(
        accountIds, today.minusDays(HISTORY_DAYS), today))
      .thenApply(transactions -> RecurringPaymentDetector.create(today)
        .detect(transactions));
  }

  private ApiResponse paymentsResponse(
    List<RecurringPayment> payments, List<RecurringSetting> settings,
    LocalDate today
  ) {
    var byKey = settings.stream().collect(Collectors.toMap(
      RecurringSetting::paymentKey, Function.identity()));
    return ApiResponse.success(Map.of("payments", payments.stream()
      .map(payment -> payment.information(today,
        Optional.ofNullable(byKey.get(payment.key()))))
      .toList()));
  }
}