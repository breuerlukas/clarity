package de.lukasbreuer.clarity.api.business.invoice.matching;

import de.lukasbreuer.clarity.api.business.invoice.Invoice;
import de.lukasbreuer.clarity.api.business.invoice.InvoicePayment;

import java.util.ArrayList;
import java.util.List;

/**
 * @param links New links to store
 * @param settled Invoices paid with an early-payment discount
 */
public record MatchResult(List<InvoicePayment> links, List<Invoice> settled) {
  static MatchResult empty() {
    return new MatchResult(new ArrayList<>(), new ArrayList<>());
  }
}