package de.lukasbreuer.clarity.api.web.transaction;

import de.lukasbreuer.clarity.api.transaction.TransactionRepository;
import de.lukasbreuer.clarity.api.transaction.category.Category;
import de.lukasbreuer.clarity.api.transaction.category.CategoryRepository;
import de.lukasbreuer.clarity.api.transaction.category.CategoryRuleRepository;
import de.lukasbreuer.clarity.api.transaction.split.TransactionSplitRepository;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Deletes an own category. Its transactions, hand-picked ones included, the
 * parts of split ones and the remembered rules pointing to it move to the
 * category the owner chose,
 * so nothing is left without one. Built-in categories can only be hidden.
 * Error codes: 3002 unknown category, 3004 built-in category, 3005 invalid
 * target category.
 */
@RestController
@RequiredArgsConstructor
public final class CategoryRemovalController {
  private final CategoryRepository categoryRepository;
  private final TransactionRepository transactionRepository;
  private final CategoryRuleRepository ruleRepository;
  private final TransactionSplitRepository splitRepository;

  @RequestMapping(path = "/categories/delete/", method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> delete(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var key = body.getString("key", 64);
    var target = body.getString("target", 64);
    if (key.equals(target)) {
      return ApiResponse.error(3005).future();
    }
    return categoryRepository.findById(key)
      .thenCompose(category -> categoryRepository.existsById(target)
        .thenCompose(targetExists -> delete(category, target, targetExists)));
  }

  private CompletableFuture<ApiResponse> delete(
    Optional<Category> category, String target, boolean targetExists
  ) {
    if (category.isEmpty()) {
      return ApiResponse.error(3002).future();
    }
    if (category.get().builtIn()) {
      return ApiResponse.error(3004).future();
    }
    if (!targetExists) {
      return ApiResponse.error(3005).future();
    }
    var moved = transactionRepository.moveCategory(category.get().key(),
      target);
    ruleRepository.moveCategory(category.get().key(), target);
    splitRepository.moveCategory(category.get().key(), target);
    return categoryRepository.delete(category.get())
      .thenApply(_ -> ApiResponse.success(Map.of("moved", moved)));
  }
}