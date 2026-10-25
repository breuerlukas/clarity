package de.lukasbreuer.clarity.api.web.transaction;

import de.lukasbreuer.clarity.api.space.SpaceRepository;
import de.lukasbreuer.clarity.api.transaction.budget.CategoryBudget;
import de.lukasbreuer.clarity.api.transaction.budget.CategoryBudgetRepository;
import de.lukasbreuer.clarity.api.transaction.category.Category;
import de.lukasbreuer.clarity.api.transaction.category.CategoryKind;
import de.lukasbreuer.clarity.api.transaction.category.CategoryRepository;
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

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The budgets of a space and setting one. An empty limit removes the
 * budget of that category. Error codes: 3002 unknown category, 3007
 * invalid limit or not a spending category, 5001 unknown space.
 */
@RestController
@RequiredArgsConstructor
public final class CategoryBudgetController {
  private static final BigDecimal MAXIMUM_LIMIT = new BigDecimal("1000000000");

  private final SpaceRepository spaceRepository;
  private final CategoryRepository categoryRepository;
  private final CategoryBudgetRepository budgetRepository;

  @RequestMapping(path = "/budgets/", method = RequestMethod.GET)
  public CompletableFuture<ApiResponse> findBudgets(
    @RequestParam("space") String space
  ) {
    var spaceId = UuidParameter.parse(space);
    if (spaceId.isEmpty()) {
      return ApiResponse.error(5001).future();
    }
    return budgetRepository.findBySpaceId(spaceId.get())
      .thenApply(budgets -> ApiResponse.success(Map.of("budgets",
        budgets.stream().map(CategoryBudget::information).toList())));
  }

  @RequestMapping(path = "/budgets/update/", method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> update(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var spaceId = body.getUUID("space");
    var key = body.getString("category", 64);
    var limit = body.getString("limit", 24);
    if (spaceId == null) {
      return ApiResponse.error(5001).future();
    }
    if (!limit.isEmpty() && parseLimit(limit).isEmpty()) {
      return ApiResponse.error(3007).future();
    }
    return spaceRepository.existsById(spaceId)
      .thenCompose(exists -> exists ? categoryRepository.findById(key)
        .thenCompose(category -> update(spaceId, category, parseLimit(limit)))
        : ApiResponse.error(5001).future());
  }

  private Optional<BigDecimal> parseLimit(String limit) {
    try {
      return Optional.of(new BigDecimal(limit))
        .filter(amount -> amount.signum() > 0)
        .filter(amount -> amount.stripTrailingZeros().scale() <= 2)
        .filter(amount -> amount.compareTo(MAXIMUM_LIMIT) < 0);
    } catch (NumberFormatException exception) {
      return Optional.empty();
    }
  }

  private CompletableFuture<ApiResponse> update(
    UUID spaceId, Optional<Category> category, Optional<BigDecimal> limit
  ) {
    if (category.isEmpty()) {
      return ApiResponse.error(3002).future();
    }
    if (category.get().kind() != CategoryKind.SPENDING) {
      return ApiResponse.error(3007).future();
    }
    return budgetRepository.findBySpaceIdAndCategory(spaceId,
        category.get().key())
      .thenCompose(budget -> limit.isEmpty() ? remove(budget) :
        save(budget.orElseGet(() -> CategoryBudget.create(UUID.randomUUID(),
          spaceId, category.get().key(), limit.get())), limit.get()));
  }

  private CompletableFuture<ApiResponse> remove(
    Optional<CategoryBudget> budget
  ) {
    return budget.map(budgetRepository::delete)
      .orElse(CompletableFuture.completedFuture(null))
      .thenApply(_ -> ApiResponse.success());
  }

  private CompletableFuture<ApiResponse> save(
    CategoryBudget budget, BigDecimal limit
  ) {
    budget.changeLimit(limit);
    return budgetRepository.save(budget)
      .thenApply(saved -> ApiResponse.success(Map.of("budget",
        saved.information())));
  }
}