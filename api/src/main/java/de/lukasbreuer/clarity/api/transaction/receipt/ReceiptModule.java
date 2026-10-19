package de.lukasbreuer.clarity.api.transaction.receipt;

import org.apache.commons.configuration2.INIConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReceiptModule {
  @Bean
  ReceiptConfiguration receiptConfiguration(INIConfiguration file) {
    var configuration = ReceiptConfiguration.create();
    configuration.load(file);
    return configuration;
  }

  /**
   * Read once at startup; a broken vocabulary file stops the boot.
   */
  @Bean
  ReceiptVocabulary receiptVocabulary() {
    return ReceiptVocabularyFile.load().vocabulary();
  }
}