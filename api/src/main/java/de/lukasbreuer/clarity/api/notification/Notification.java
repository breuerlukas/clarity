package de.lukasbreuer.clarity.api.notification;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * One thing worth a mail. The event key names the occurrence, not the kind:
 * a consent that runs out gets one key per expiry date, so a renewed one
 * is announced again when it runs out the next time, but never twice.
 * Values are dates, amounts and names the text fills in.
 */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(staticName = "create")
public final class Notification {
  private final NotificationKind kind;
  private final String eventKey;
  private final Map<String, Object> values;
}
