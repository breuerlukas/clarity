package de.lukasbreuer.clarity.api.banking;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * A request to Enable Banking that came back with an error status. Only the
 * status code is kept: the body may echo account data, and that must never
 * end up in a log.
 */
@Getter
@Accessors(fluent = true)
public final class EnableBankingException extends RuntimeException {
  private final int status;

  public EnableBankingException(int status) {
    super("Enable Banking answered with status " + status);
    this.status = status;
  }

  /**
   * 401 and 403 on account endpoints mean the bank consent expired or was
   * revoked; only a new connection fixes that.
   */
  public boolean isConsentProblem() {
    return status == 401 || status == 403;
  }
}