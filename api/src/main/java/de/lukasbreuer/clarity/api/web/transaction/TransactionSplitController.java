package de.lukasbreuer.clarity.api.web.transaction;

import de.lukasbreuer.clarity.api.transaction.Transaction;
import de.lukasbreuer.clarity.api.transaction.TransactionRepository;
import de.lukasbreuer.clarity.api.transaction.category.Category;
import de.lukasbreuer.clarity.api.transaction.category.CategoryRepository;
import de.lukasbreuer.clarity.api.transaction.split.SplitAmounts;
import de.lukasbreuer.clarity.api.transaction.split.TransactionSplit;
import de.lukasbreuer.clarity.api.transaction.split.TransactionSplitRepository;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Splits a transaction into several categories, usually along the items of
 * its receipt; an empty list of parts undoes the split. Error codes: 3001
 * unknown transaction, 3002 unknown category, 3006 parts that do not add up
 * to the transaction.
 */
@RestController
@RequiredArgsConstructor
public final class TransactionSplitController {
  private final TransactionRepository transactionRepository;
  private final TransactionSplitRepository splitRepository;
  private final CategoryRepository categoryRepository;

  @RequestMapping(path = "/transactions/split/", method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> split(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var transactionId = body.getUUID("id");
    var parts = SplitPart.parseAll(body.getBodyList("splits"));
    if (transactionId == null) {
      return ApiResponse.error(3001).future();
    }
    if (parts.isEmpty()) {
      return ApiResponse.error(3006).future();
    }
    return categoryRepository.findAll()
      .thenCompose(categories -> knowsAll(categories, parts.get()) ?
        transactionRepository.findById(transactionId)
          .thenCompose(transaction -> split(transaction, parts.get())) :
        ApiResponse.error(3002).future());
  }

  private boolean knowsAll(List<Category> categories, List<SplitPart> parts) {
    var keys = categories.stream().map(Category::key)
      .collect(Collectors.toSet());
    return parts.stream().allMatch(part -> keys.contains(part.category()));
  }

  private CompletableFuture<ApiResponse> split(
    Optional<Transaction> transaction, List<SplitPart> parts
  ) {
    if (transaction.isEmpty()) {
      return ApiResponse.error(3001).future();
    }
    if (!SplitAmounts.of(transaction.get()).fit(parts.stream()
      .map(SplitPart::amount).toList())) {
      return ApiResponse.error(3006).future();
    }
    return replaceSplit(transaction.get().id(), parts)
      .thenApply(splits -> ApiResponse.success(Map.of("splits", splits.stream()
        .map(TransactionSplit::information).toList())));
  }

  private CompletableFuture<List<TransactionSplit>> replaceSplit(
    UUID transactionId, List<SplitPart> parts
  ) {
    splitRepository.deleteByTransactionId(transactionId);
    return splitRepository.saveAll(parts.stream()
      .map(part -> part.create(transactionId)).toList());
  }
}