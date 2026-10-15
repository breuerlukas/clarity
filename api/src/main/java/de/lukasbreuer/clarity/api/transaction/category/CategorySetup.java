package de.lukasbreuer.clarity.api.transaction.category;

import de.lukasbreuer.clarity.api.log.Log;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Prepares the categories on start: drops the check that limited the
 * category columns to the former enum names, then adds every built-in
 * category the database doesn't have yet. That is all of them on the first
 * start after the move from the enum, and later built-ins on updates; stored
 * ones keep the owner's changes.
 */
@Component
@RequiredArgsConstructor
public final class CategorySetup {
  private final Log log;
  private final DefaultCategories defaults;
  private final CategoryRepository categoryRepository;

  @EventListener(ApplicationReadyEvent.class)
  void prepare() {
    categoryRepository.dropTransactionCategoryCheck();
    categoryRepository.dropRuleCategoryCheck();
    categoryRepository.findAll().thenCompose(this::addMissing).join();
  }

  private CompletableFuture<List<Category>> addMissing(List<Category> stored) {
    var storedKeys = stored.stream().map(Category::key)
      .collect(Collectors.toSet());
    var missing = defaults.all().stream()
      .filter(category -> !storedKeys.contains(category.key()))
      .toList();
    if (!missing.isEmpty()) {
      log.info("Adding " + missing.size() + " built-in categories");
    }
    return categoryRepository.saveAll(missing);
  }
}