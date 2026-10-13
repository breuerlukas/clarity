package de.lukasbreuer.clarity.api.space;

import java.util.Arrays;
import java.util.Optional;

/**
 * What a space holds: the owner's own money, or a business with customer
 * contracts. Business spaces sign in at the bank as a company.
 */
public enum SpaceKind {
  PERSONAL,
  BUSINESS;

  public static Optional<SpaceKind> find(String name) {
    return Arrays.stream(values())
      .filter(kind -> kind.name().equals(name))
      .findFirst();
  }
}
