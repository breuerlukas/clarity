package de.lukasbreuer.clarity.api.transaction.category;

import lombok.RequiredArgsConstructor;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * The keywords of every visible category, first match wins. Own categories
 * go first: their keywords are the owner's and more specific than the
 * built-in ones. The built-in ones follow in their shipped order, so
 * "amazon prime" (subscriptions) is tried before "amazon" (shopping).
 */
@RequiredArgsConstructor(staticName = "create")
public final class CategoryKeywords {
  private final List<Category> categories;

  public Optional<String> find(String text) {
    return categories.stream()
      .filter(category -> !category.hidden())
      .sorted(Comparator.comparing(Category::builtIn)
        .thenComparing(Category::position))
      .filter(category -> category.keywords().stream().anyMatch(keyword ->
        KeywordMatch.of(text, keyword).matches()))
      .map(Category::key)
      .findFirst();
  }
}