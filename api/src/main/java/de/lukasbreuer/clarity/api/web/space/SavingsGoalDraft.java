package de.lukasbreuer.clarity.api.web.space;

import de.lukasbreuer.clarity.api.space.goal.SavingsGoal;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * A savings goal as the panel sends it, checked: a name, an icon, a target
 * above zero with at most cents, an optional target date and an optional
 * account whose balance counts towards it.
 */
record SavingsGoalDraft(
  String name, String icon, BigDecimal target, LocalDate targetDate,
  UUID accountId
) {
  private static final BigDecimal MAXIMUM_TARGET = new BigDecimal("1000000000");

  static Optional<SavingsGoalDraft> parse(ApiRequestBody body) {
    var targetDate = body.getString("target_date", 10);
    var account = body.getString("account", 36);
    try {
      var draft = new SavingsGoalDraft(body.getString("name", 60).trim(),
        body.getString("icon", 40),
        new BigDecimal(body.getString("target", 24)),
        targetDate.isEmpty() ? null : LocalDate.parse(targetDate),
        account.isEmpty() ? null : UUID.fromString(account));
      return Optional.of(draft).filter(SavingsGoalDraft::isValid);
    } catch (Exception exception) {
      return Optional.empty();
    }
  }

  private boolean isValid() {
    return !name.isEmpty() && !icon.isEmpty() && target.signum() > 0 &&
      target.stripTrailingZeros().scale() <= 2 &&
      target.compareTo(MAXIMUM_TARGET) < 0;
  }

  SavingsGoal create(UUID spaceId) {
    return SavingsGoal.create(UUID.randomUUID(), spaceId, name, icon, target,
      targetDate, accountId, BigDecimal.ZERO, System.currentTimeMillis());
  }

  SavingsGoal applyTo(SavingsGoal goal) {
    goal.edit(name, icon, target, targetDate, accountId);
    return goal;
  }
}
