package de.lukasbreuer.clarity.api.web.space;

import de.lukasbreuer.clarity.api.space.Space;
import de.lukasbreuer.clarity.api.space.SpaceRepository;
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
 * Sets what the owner wants to spend per pay cycle in a space, beyond the
 * fixed costs. An empty amount goes back to the panel's suggestion. Error
 * codes: 5001 unknown space, 5004 invalid amount.
 */
@RestController
@RequiredArgsConstructor
public final class SpaceBudgetController {
  private static final BigDecimal MAXIMUM_BUDGET = new BigDecimal("1000000000");

  private final SpaceRepository spaceRepository;

  @RequestMapping(path = "/spaces/budget/", method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> changeBudget(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var spaceId = body.getUUID("id");
    var amount = body.getString("amount", 24);
    var budget = parseBudget(amount);
    if (spaceId == null) {
      return ApiResponse.error(5001).future();
    }
    if (!amount.isEmpty() && budget.isEmpty()) {
      return ApiResponse.error(5004).future();
    }
    return spaceRepository.findById(spaceId)
      .thenCompose(space -> space.map(found -> save(found, budget))
        .orElse(ApiResponse.error(5001).future()));
  }

  private Optional<BigDecimal> parseBudget(String amount) {
    try {
      return Optional.of(new BigDecimal(amount))
        .filter(value -> value.signum() > 0)
        .filter(value -> value.stripTrailingZeros().scale() <= 2)
        .filter(value -> value.compareTo(MAXIMUM_BUDGET) < 0);
    } catch (NumberFormatException exception) {
      return Optional.empty();
    }
  }

  private CompletableFuture<ApiResponse> save(
    Space space, Optional<BigDecimal> budget
  ) {
    space.changeBudget(budget.orElse(null));
    return spaceRepository.save(space)
      .thenApply(saved -> ApiResponse.success(Map.of("space",
        saved.information())));
  }
}
