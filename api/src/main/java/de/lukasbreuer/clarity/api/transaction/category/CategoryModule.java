package de.lukasbreuer.clarity.api.transaction.category;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CategoryModule {
  /**
   * Read once at startup; a broken or missing keyword file stops the boot
   * instead of leaving every transaction uncategorized.
   */
  @Bean
  CategoryKeywords categoryKeywords() {
    return CategoryKeywords.load();
  }
}
