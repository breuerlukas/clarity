package de.lukasbreuer.clarity.api.web.transaction;

import de.lukasbreuer.clarity.api.transaction.recurring.RecurringSetting;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * The owner's decisions about a fixed cost as the panel sends them, checked.
 * An empty value clears it: no end date, no dismissal, no notice period.
 */
record RecurringSettingDraft(
  LocalDate cancelledOn, LocalDate dismissedLastDate, Integer noticeDays
) {
  private static final int MAXIMUM_NOTICE_DAYS = 366;

  static Optional<RecurringSettingDraft> parse(ApiRequestBody body) {
    var cancelledOn = body.getString("cancelled_on", 10);
    var dismissed = body.getString("dismissed_last_date", 10);
    var noticeDays = body.getString("notice_days", 3);
    try {
      var draft = new RecurringSettingDraft(
        cancelledOn.isEmpty() ? null : LocalDate.parse(cancelledOn),
        dismissed.isEmpty() ? null : LocalDate.parse(dismissed),
        noticeDays.isEmpty() ? null : Integer.valueOf(noticeDays));
      return Optional.of(draft).filter(RecurringSettingDraft::isValid);
    } catch (Exception exception) {
      return Optional.empty();
    }
  }

  private boolean isValid() {
    return noticeDays == null ||
      (noticeDays >= 0 && noticeDays <= MAXIMUM_NOTICE_DAYS);
  }

  RecurringSetting applyTo(RecurringSetting setting) {
    setting.change(cancelledOn, dismissedLastDate, noticeDays);
    return setting;
  }

  RecurringSetting create(UUID spaceId, String key) {
    return applyTo(RecurringSetting.create(UUID.randomUUID(), spaceId, key,
      null, null, null));
  }
}
