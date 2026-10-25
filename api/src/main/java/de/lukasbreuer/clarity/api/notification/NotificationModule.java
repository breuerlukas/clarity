package de.lukasbreuer.clarity.api.notification;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationModule {
  /**
   * Read once at startup; a broken texts file stops the boot instead of
   * failing the first notification hours later.
   */
  @Bean
  NotificationTexts notificationTexts() {
    return NotificationTexts.load();
  }
}
