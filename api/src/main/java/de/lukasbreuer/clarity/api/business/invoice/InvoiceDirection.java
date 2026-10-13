package de.lukasbreuer.clarity.api.business.invoice;

import java.util.Arrays;
import java.util.Optional;

/**
 * Incoming invoices are bills the business pays; outgoing ones it sent to
 * customers, who pay them.
 */
public enum InvoiceDirection {
  INCOMING,
  OUTGOING;

  public static Optional<InvoiceDirection> find(String name) {
    return Arrays.stream(values())
      .filter(direction -> direction.name().equals(name))
      .findFirst();
  }
}
