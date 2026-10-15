package de.lukasbreuer.clarity.api.transaction.category;

import de.lukasbreuer.clarity.api.database.DatabaseRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * The two drops are synchronous like every modifying query. While categories
 * were an enum, Hibernate guarded the category columns with a check against
 * the enum names, which would refuse every own category.
 */
@Repository
public interface CategoryRepository
  extends DatabaseRepository<Category, String> {
  @Modifying
  @Transactional
  @Query(value = "alter table if exists transactions " +
    "drop constraint if exists transactions_category_check",
    nativeQuery = true)
  void dropTransactionCategoryCheck();

  @Modifying
  @Transactional
  @Query(value = "alter table if exists category_rule " +
    "drop constraint if exists category_rule_category_check",
    nativeQuery = true)
  void dropRuleCategoryCheck();
}