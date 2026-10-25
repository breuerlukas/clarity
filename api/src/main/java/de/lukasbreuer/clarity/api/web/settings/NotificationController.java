package de.lukasbreuer.clarity.api.web.settings;

import de.lukasbreuer.clarity.api.notification.Notification;
import de.lukasbreuer.clarity.api.notification.NotificationDispatch;
import de.lukasbreuer.clarity.api.notification.NotificationKind;
import de.lukasbreuer.clarity.api.notification.NotificationSettings;
import de.lukasbreuer.clarity.api.notification.NotificationSettingsRepository;
import de.lukasbreuer.clarity.api.secret.SecretBox;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Where notifications go, which ones, and a test mail through the saved
 * settings. Error codes: 1007 invalid settings or no password yet, 1008
 * the test mail could not be sent, 1009 nothing saved yet.
 */
@RestController
@RequiredArgsConstructor
public final class NotificationController {
  private final NotificationSettingsRepository settingsRepository;
  private final NotificationDispatch dispatch;
  private final SecretBox secretBox;

  @RequestMapping(path = "/notifications/", method = RequestMethod.GET)
  public CompletableFuture<ApiResponse> findSettings() {
    return settingsRepository.findFirstByIdNotNull()
      .thenApply(settings -> ApiResponse.success(Map.of("settings",
        settings.map(NotificationSettings::information).orElse(Map.of()))));
  }

  @RequestMapping(path = "/notifications/update/",
    method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> update(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var draft = NotificationDraft.parse(ApiRequestBody.of(payload, response));
    if (draft.isEmpty()) {
      return ApiResponse.error(1007).future();
    }
    return settingsRepository.findFirstByIdNotNull()
      .thenCompose(settings -> save(settings, draft.get()));
  }

  private CompletableFuture<ApiResponse> save(
    Optional<NotificationSettings> stored, NotificationDraft draft
  ) {
    if (stored.isEmpty() && !draft.hasPassword()) {
      return ApiResponse.error(1007).future();
    }
    var settings = stored.orElse(draft.settings());
    settings.edit(draft.settings(), draft.hasPassword() ?
      secretBox.encrypt(draft.password()) : null);
    return settingsRepository.save(settings)
      .thenApply(saved -> ApiResponse.success(Map.of("settings",
        saved.information())));
  }

  @RequestMapping(path = "/notifications/test/", method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> sendTest() {
    return settingsRepository.findFirstByIdNotNull()
      .thenApply(settings -> settings.map(this::sendTest)
        .orElse(ApiResponse.error(1009)));
  }

  private ApiResponse sendTest(NotificationSettings settings) {
    try {
      dispatch.send(settings, Notification.create(NotificationKind.TEST,
        "TEST", Map.of()));
      return ApiResponse.success();
    } catch (MessagingException | IllegalStateException exception) {
      return ApiResponse.error(1008);
    }
  }
}
