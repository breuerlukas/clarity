package de.lukasbreuer.clarity.api.transaction;

import java.util.Arrays;
import java.util.Optional;

public enum Category {
  SALARY,
  INCOME,
  HOUSING,
  UTILITIES,
  GROCERIES,
  DINING,
  SHOPPING,
  MOBILITY,
  TRAVEL,
  SUBSCRIPTIONS,
  INSURANCE,
  HEALTH,
  LEISURE,
  EDUCATION,
  CASH,
  FEES,
  SAVINGS,
  TRANSFER,
  OTHER;

  public static Optional<Category> find(String name) {
    return Arrays.stream(values())
      .filter(category -> category.name().equals(name))
      .findFirst();
  }
}