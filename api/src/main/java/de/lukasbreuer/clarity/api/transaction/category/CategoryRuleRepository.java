package de.lukasbreuer.clarity.api.transaction.category;

import de.lukasbreuer.clarity.api.database.DatabaseRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRuleRepository
  extends DatabaseRepository<CategoryRule, String> {
}