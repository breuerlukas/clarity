package de.lukasbreuer.clarity.api.transaction.category;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CategoryModule {
  /**
   * Read once at startup; a broken or missing file stops the boot instead of
   * leaving the categories unseeded.
   */
  @Bean
  DefaultCategories defaultCategories() {
    return DefaultCategories.load();
  }
}
