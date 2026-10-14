package de.lukasbreuer.clarity.api.business.invoice;

/**
 * Where an invoice stands, the first that applies: paid in full (or marked
 * settled), past its due date, partly paid, announced by a payment advice,
 * or open.
 */
public enum InvoiceStatus {
  PAID,
  OVERDUE,
  PARTIAL,
  ANNOUNCED,
  OPEN
}