package de.lukasbreuer.clarity.api.notification;

import de.lukasbreuer.clarity.api.log.Log;
import de.lukasbreuer.clarity.api.secret.SecretBox;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Sends what is new every hour, once notifications are switched on. A mail
 * that fails is not marked as sent and goes out on the next run.
 */
@Component
@RequiredArgsConstructor
public final class NotificationDispatch {
  private final NotificationSettingsRepository settingsRepository;
  private final SentNotificationRepository sentRepository;
  private final NotificationSources sources;
  private final NotificationTexts texts;
  private final SmtpSender sender;
  private final SecretBox secretBox;
  private final Log log;

  @Scheduled(fixedDelay = 1, initialDelay = 1, timeUnit = TimeUnit.HOURS)
  void notifyOwner() {
    settingsRepository.findFirstByIdNotNull()
      .thenCompose(settings -> settings.filter(NotificationSettings::enabled)
        .map(this::dispatch)
        .orElse(CompletableFuture.completedFuture(null)))
      .join();
  }

  private CompletableFuture<Void> dispatch(NotificationSettings settings) {
    var events = NotificationEvents.create(Instant.now(),
      ZoneId.systemDefault());
    return sources.collect(events, LocalDate.now())
      .thenApply(notifications -> notifications.stream()
        .filter(notification -> settings.sends(notification.kind())).toList())
      .thenCompose(this::unsent)
      .thenAccept(unsent -> unsent.forEach(notification ->
        sendOnce(settings, notification)));
  }

  private CompletableFuture<List<Notification>> unsent(
    List<Notification> notifications
  ) {
    if (notifications.isEmpty()) {
      return CompletableFuture.completedFuture(List.of());
    }
    return sentRepository.findByEventKeyIn(notifications.stream()
        .map(Notification::eventKey).toList())
      .thenApply(sent -> withoutSent(notifications, sent));
  }

  private List<Notification> withoutSent(
    List<Notification> notifications, List<SentNotification> sent
  ) {
    var sentKeys = sent.stream().map(SentNotification::eventKey)
      .collect(Collectors.toSet());
    return notifications.stream()
      .filter(notification -> !sentKeys.contains(notification.eventKey()))
      .toList();
  }

  private void sendOnce(
    NotificationSettings settings, Notification notification
  ) {
    try {
      send(settings, notification);
      sentRepository.save(SentNotification.create(UUID.randomUUID(),
        notification.eventKey(), System.currentTimeMillis())).join();
    } catch (MessagingException | IllegalStateException exception) {
      log.processError(exception);
    }
  }

  public void send(NotificationSettings settings, Notification notification)
    throws MessagingException {
    sender.send(settings, secretBox.decrypt(settings.encryptedPassword()),
      texts.subject(notification, settings.language()),
      texts.text(notification, settings.language()));
  }
}
