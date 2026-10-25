package de.lukasbreuer.clarity.api.notification;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NotificationTextsTest {
  private final NotificationTexts texts = NotificationTexts.load();
  private final Notification priceChange = Notification.create(
    NotificationKind.PRICE_CHANGED, "key", Map.of("counterparty", "Netflix",
      "previous", new BigDecimal("13.99"), "amount", new BigDecimal("15.99"),
      "currency", "EUR"));

  @Test
  void writesAmountsTheWayTheLanguageDoes() {
    assertEquals("Netflix kostet jetzt 15,99 €",
      texts.subject(priceChange, "de").replace(' ', ' '));
    assertEquals("Netflix now costs €15.99",
      texts.subject(priceChange, "en"));
  }

  @Test
  void fillsDatesAndFallsBackToEnglish() {
    var expiring = Notification.create(NotificationKind.CONSENT_EXPIRING,
      "key", Map.of("bank", "DKB", "date", LocalDate.of(2026, 11, 3)));
    assertTrue(texts.text(expiring, "de").contains("03.11.2026"));
    assertTrue(texts.text(expiring, "fr").contains("Nov 3, 2026"));
  }

  @Test
  void hasEveryKindInEveryLanguage() {
    var values = Map.<String, Object>of();
    for (var kind : NotificationKind.values()) {
      for (var language : new String[]{"de", "en"}) {
        texts.subject(Notification.create(kind, "key", values), language);
      }
    }
  }
}
