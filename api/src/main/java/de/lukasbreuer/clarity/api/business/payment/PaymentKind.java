package de.lukasbreuer.clarity.api.business.payment;

import java.util.Arrays;
import java.util.Optional;

/**
 * What a booking on a business account is: a customer paying a contract,
 * money the owner takes out for themselves (Privatentnahme), money they put
 * in (Einlage), or none of these.
 */
public enum PaymentKind {
  CONTRACT,
  WITHDRAWAL,
  DEPOSIT,
  NONE;

  public static Optional<PaymentKind> find(String name) {
    return Arrays.stream(values())
      .filter(kind -> kind.name().equals(name))
      .findFirst();
  }
}
