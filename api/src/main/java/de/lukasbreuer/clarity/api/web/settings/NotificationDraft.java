package de.lukasbreuer.clarity.api.web.settings;

import de.lukasbreuer.clarity.api.business.mail.MailSecurity;
import de.lukasbreuer.clarity.api.notification.NotificationSettings;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;
import jakarta.mail.internet.InternetAddress;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The notification settings as the panel sends them, checked: an SMTP host
 * and port, TLS or STARTTLS, a user name, valid sender and recipient
 * addresses and a known language. An empty password keeps the stored one.
 */
record NotificationDraft(NotificationSettings settings, String password) {
  private static final String HOST_PATTERN = "[A-Za-z0-9.:\\-\\[\\]]{1,253}";
  private static final Set<String> LANGUAGES = Set.of("de", "en");

  static Optional<NotificationDraft> parse(ApiRequestBody body) {
    try {
      var settings = NotificationSettings.create(UUID.randomUUID(),
        body.getBoolean("enabled"), body.getString("host", 253).trim(),
        (int) body.getLong("port"),
        MailSecurity.find(body.getString("security", 16)).orElseThrow(),
        body.getString("username", 200).trim(), "",
        address(body.getString("sender", 254)),
        address(body.getString("recipient", 254)),
        body.getString("language", 2), body.getBoolean("consent_events"),
        body.getBoolean("sync_events"), body.getBoolean("price_events"));
      return Optional.of(new NotificationDraft(settings,
        body.getString("password", 500))).filter(NotificationDraft::isValid);
    } catch (Exception exception) {
      return Optional.empty();
    }
  }

  private static String address(String value) throws Exception {
    return new InternetAddress(value.trim(), true).getAddress();
  }

  private boolean isValid() {
    return settings.host().matches(HOST_PATTERN) && settings.port() > 0 &&
      settings.port() <= 65535 && !settings.username().isEmpty() &&
      LANGUAGES.contains(settings.language());
  }

  boolean hasPassword() {
    return !password.isEmpty();
  }
}
