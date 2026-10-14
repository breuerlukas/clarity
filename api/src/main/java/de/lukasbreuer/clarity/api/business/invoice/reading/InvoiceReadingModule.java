package de.lukasbreuer.clarity.api.business.invoice.reading;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InvoiceReadingModule {
  /**
   * Read once at startup; a broken vocabulary file stops the boot instead
   * of leaving every invoice without e-invoice data unread.
   */
  @Bean
  InvoiceVocabulary invoiceVocabulary() {
    return InvoiceVocabularyFile.load().vocabulary();
  }
}