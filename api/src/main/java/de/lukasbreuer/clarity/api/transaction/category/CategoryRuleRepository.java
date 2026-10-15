package de.lukasbreuer.clarity.api.transaction.category;

import de.lukasbreuer.clarity.api.database.DatabaseRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface CategoryRuleRepository
  extends DatabaseRepository<CategoryRule, String> {
  /**
   * Synchronous: Spring Data only allows void or a row count as the result of
   * a modifying query.
   */
  @Modifying
  @Transactional
  @Query("update CategoryRule rule set rule.category = :target " +
    "where rule.category = :source")
  int moveCategory(
    @Param("source") String source, @Param("target") String target);
}