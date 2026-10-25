package de.lukasbreuer.clarity.api.notification;

/**
 * What the owner is told about. Each has a subject and a text per language
 * in notification-texts.json.
 */
public enum NotificationKind {
  CONSENT_EXPIRING,
  CONSENT_EXPIRED,
  SYNC_FAILING,
  PRICE_CHANGED,
  TEST
}
