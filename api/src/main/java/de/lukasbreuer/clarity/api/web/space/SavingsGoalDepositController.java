package de.lukasbreuer.clarity.api.web.space;

import de.lukasbreuer.clarity.api.space.goal.SavingsGoal;
import de.lukasbreuer.clarity.api.space.goal.SavingsGoalRepository;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Sets money aside for a goal by hand, or takes some back with a negative
 * amount; never more than was set aside. Error codes: 8200 unknown goal,
 * 8203 invalid amount.
 */
@RestController
@RequiredArgsConstructor
public final class SavingsGoalDepositController {
  private static final BigDecimal MAXIMUM_AMOUNT = new BigDecimal("1000000000");

  private final SavingsGoalRepository goalRepository;

  @RequestMapping(path = "/goals/deposit/", method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> deposit(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var goalId = body.getUUID("id");
    var amount = parseAmount(body.getString("amount", 24));
    if (goalId == null) {
      return ApiResponse.error(8200).future();
    }
    if (amount.isEmpty()) {
      return ApiResponse.error(8203).future();
    }
    return goalRepository.findById(goalId)
      .thenCompose(goal -> deposit(goal, amount.get()));
  }

  private Optional<BigDecimal> parseAmount(String amount) {
    try {
      return Optional.of(new BigDecimal(amount))
        .filter(value -> value.signum() != 0)
        .filter(value -> value.stripTrailingZeros().scale() <= 2)
        .filter(value -> value.abs().compareTo(MAXIMUM_AMOUNT) < 0);
    } catch (NumberFormatException exception) {
      return Optional.empty();
    }
  }

  private CompletableFuture<ApiResponse> deposit(
    Optional<SavingsGoal> goal, BigDecimal amount
  ) {
    if (goal.isEmpty()) {
      return ApiResponse.error(8200).future();
    }
    if (!goal.get().allowsDeposit(amount)) {
      return ApiResponse.error(8203).future();
    }
    goal.get().deposit(amount);
    return goalRepository.save(goal.get())
      .thenApply(saved -> ApiResponse.success(Map.of("goal",
        saved.information())));
  }
}
