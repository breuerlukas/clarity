package de.lukasbreuer.clarity.api.transaction.recurring;

/**
 * Whether a fixed cost still runs. Missing: its booking is overdue by more
 * than the cycle allows. Ended: a second cycle passed without one, or the
 * owner cancelled it and nothing is due before the end date any more.
 */
public enum RecurringStatus {
  ACTIVE,
  MISSING,
  ENDED
}
