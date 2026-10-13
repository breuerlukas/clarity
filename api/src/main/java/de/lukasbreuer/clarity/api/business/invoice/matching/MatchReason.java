package de.lukasbreuer.clarity.api.business.invoice.matching;

/**
 * Why a booking may pay an invoice: its reference names the invoice number,
 * it is exactly the amount still open, it comes from or goes to the IBAN on
 * the invoice, or the other side's name matches.
 */
public enum MatchReason {
  NUMBER,
  AMOUNT,
  IBAN,
  NAME
}
