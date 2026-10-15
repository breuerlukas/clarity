package de.lukasbreuer.clarity.api.web.transaction;

import de.lukasbreuer.clarity.api.transaction.category.Category;
import de.lukasbreuer.clarity.api.transaction.category.CategoryRepository;
import de.lukasbreuer.clarity.api.transaction.category.DefaultCategories;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Lists, creates and changes the categories, and resets a built-in one to
 * how it shipped. They are shared by every space, like the remembered rules.
 * Error codes: 3002 unknown category, 3003 invalid category, 3004 a system
 * category can't be hidden.
 */
@RestController
@RequiredArgsConstructor
public final class CategoryController {
  private final CategoryRepository categoryRepository;
  private final DefaultCategories defaults;

  @RequestMapping(path = "/categories/", method = RequestMethod.GET)
  public CompletableFuture<ApiResponse> findCategories() {
    return categoryRepository.findAll()
      .thenApply(categories -> ApiResponse.success(Map.of("categories",
        categories.stream()
          .sorted(Comparator.comparing(Category::position))
          .map(Category::information)
          .toList())));
  }

  @RequestMapping(path = "/categories/create/", method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> create(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var draft = CategoryDraft.parse(ApiRequestBody.of(payload, response));
    if (draft.isEmpty() || draft.get().chosenName() == null) {
      return ApiResponse.error(3003).future();
    }
    return categoryRepository.findAll()
      .thenCompose(categories -> save(draft.get()
        .create(nextPosition(categories))));
  }

  @RequestMapping(path = "/categories/update/", method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> update(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var key = body.getString("key", 64);
    var draft = CategoryDraft.parse(body);
    var hidden = body.getBoolean("hidden");
    if (draft.isEmpty()) {
      return ApiResponse.error(3003).future();
    }
    return categoryRepository.findById(key)
      .thenCompose(category -> update(category, draft.get(), hidden));
  }

  @RequestMapping(path = "/categories/reset/", method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> reset(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var key = ApiRequestBody.of(payload, response).getString("key", 64);
    var original = defaults.find(key);
    if (original.isEmpty()) {
      return ApiResponse.error(3002).future();
    }
    return categoryRepository.findById(key)
      .thenCompose(category -> reset(category, original.get()));
  }

  /**
   * Own categories need a name, as they have no translated label.
   */
  private CompletableFuture<ApiResponse> update(
    Optional<Category> category, CategoryDraft draft, boolean hidden
  ) {
    if (category.isEmpty()) {
      return ApiResponse.error(3002).future();
    }
    if (hidden && category.get().isSystem()) {
      return ApiResponse.error(3004).future();
    }
    if (!category.get().builtIn() && draft.chosenName() == null) {
      return ApiResponse.error(3003).future();
    }
    draft.applyTo(category.get());
    category.get().hide(hidden);
    return save(category.get());
  }

  private CompletableFuture<ApiResponse> reset(
    Optional<Category> category, Category original
  ) {
    if (category.isEmpty()) {
      return ApiResponse.error(3002).future();
    }
    category.get().resetTo(original);
    return save(category.get());
  }

  private CompletableFuture<ApiResponse> save(Category category) {
    return categoryRepository.save(category)
      .thenApply(saved -> ApiResponse.success(Map.of("category",
        saved.information())));
  }

  private int nextPosition(List<Category> categories) {
    return categories.stream().mapToInt(Category::position).max()
      .orElse(-1) + 1;
  }
}