package de.lukasbreuer.clarity.api.web.request;

import java.util.Optional;
import java.util.UUID;

public final class UuidParameter {
  private UuidParameter() {
  }

  /**
   * @return The uuid in a query parameter, or empty if it is none
   */
  public static Optional<UUID> parse(String value) {
    try {
      return Optional.of(UUID.fromString(value));
    } catch (Exception exception) {
      return Optional.empty();
    }
  }
}
