package de.lukasbreuer.clarity.api.transaction.category;

import java.util.Arrays;
import java.util.Optional;

/**
 * Whether money in a category usually goes out or comes in; the panel offers
 * the fitting categories first when one is picked for a transaction.
 */
public enum CategoryKind {
  SPENDING,
  INCOME;

  public static Optional<CategoryKind> find(String name) {
    return Arrays.stream(values())
      .filter(kind -> kind.name().equals(name))
      .findFirst();
  }
}