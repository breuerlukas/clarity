package de.lukasbreuer.clarity.api.notification;

import lombok.RequiredArgsConstructor;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Currency;
import java.util.Locale;
import java.util.Map;

/**
 * The subject and text of a notification in the owner's language, from
 * {@code notification-texts.json}. Dates and amounts are written the way
 * that language writes them; anything but German gets English.
 */
@RequiredArgsConstructor(staticName = "of")
public final class NotificationTexts {
  private static final String RESOURCE = "/notification-texts.json";

  public static NotificationTexts load() {
    try (var stream = NotificationTexts.class.getResourceAsStream(RESOURCE)) {
      if (stream == null) {
        throw new IllegalStateException("Missing " + RESOURCE);
      }
      return of(new JSONObject(new String(stream.readAllBytes(),
        StandardCharsets.UTF_8)));
    } catch (IOException exception) {
      throw new IllegalStateException("Unreadable " + RESOURCE, exception);
    }
  }

  private final JSONObject file;

  public String subject(Notification notification, String language) {
    return fill(entry(notification, language).getString("subject"),
      notification, language);
  }

  public String text(Notification notification, String language) {
    return fill(entry(notification, language).getString("text"),
      notification, language);
  }

  private JSONObject entry(Notification notification, String language) {
    return file.getJSONObject(locale(language).getLanguage())
      .getJSONObject(notification.kind().name());
  }

  private String fill(
    String template, Notification notification, String language
  ) {
    var filled = template;
    for (Map.Entry<String, Object> value : notification.values().entrySet()) {
      filled = filled.replace("{" + value.getKey() + "}",
        format(value.getValue(), notification, locale(language)));
    }
    return filled;
  }

  private String format(
    Object value, Notification notification, Locale locale
  ) {
    return switch (value) {
      case LocalDate date -> date.format(DateTimeFormatter
        .ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale));
      case BigDecimal amount -> money(amount,
        String.valueOf(notification.values().get("currency")), locale);
      default -> String.valueOf(value);
    };
  }

  private String money(BigDecimal amount, String currency, Locale locale) {
    var format = NumberFormat.getCurrencyInstance(locale);
    format.setCurrency(Currency.getInstance(currency));
    return format.format(amount);
  }

  private Locale locale(String language) {
    return "de".equals(language) ? Locale.GERMAN : Locale.ENGLISH;
  }
}
