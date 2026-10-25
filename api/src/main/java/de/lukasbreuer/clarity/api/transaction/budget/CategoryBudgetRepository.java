package de.lukasbreuer.clarity.api.transaction.budget;

import de.lukasbreuer.clarity.api.database.DatabaseRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Repository
public interface CategoryBudgetRepository
  extends DatabaseRepository<CategoryBudget, UUID> {
  @Async
  CompletableFuture<List<CategoryBudget>> findBySpaceId(UUID spaceId);

  @Async
  CompletableFuture<Optional<CategoryBudget>> findBySpaceIdAndCategory(
    UUID spaceId, String category);

  /**
   * Synchronous on purpose: Spring Data only allows void or a row count as
   * the result of a modifying query. Follows a deleted category; its
   * budget is not moved, as the target may have one of its own.
   */
  @Modifying
  @Transactional
  @Query("delete from CategoryBudget budget " +
    "where budget.category = :category")
  void deleteByCategory(@Param("category") String category);
}