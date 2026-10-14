package de.lukasbreuer.clarity.api.web.business;

import de.lukasbreuer.clarity.api.business.invoice.Invoice;
import de.lukasbreuer.clarity.api.business.invoice.InvoiceRepository;
import de.lukasbreuer.clarity.api.business.invoice.matching.PaymentReconciliation;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Answers with invoices the way the panel shows them: with the bookings
 * that paid them and their status as of today.
 */
@Component
@RequiredArgsConstructor
final class InvoiceResponses {
  private final InvoiceRepository invoiceRepository;
  private final PaymentReconciliation reconciliation;

  /**
   * Newest invoice first.
   */
  ApiResponse list(List<Invoice> invoices) {
    var payments = reconciliation.linksOf(invoices);
    var today = LocalDate.now();
    return ApiResponse.success(Map.of("invoices", invoices.stream()
      .sorted(Comparator.comparing(Invoice::invoiceDate).reversed())
      .map(invoice -> invoice.information(payments, today))
      .toList()));
  }

  /**
   * The invoice as stored now, after the matching may have changed it.
   */
  CompletableFuture<ApiResponse> current(UUID invoiceId) {
    return invoiceRepository.findById(invoiceId)
      .thenApply(invoice -> single(invoice.orElseThrow()));
  }

  private ApiResponse single(Invoice invoice) {
    var payments = reconciliation.linksOf(List.of(invoice));
    return ApiResponse.success(Map.of("invoice",
      invoice.information(payments, LocalDate.now())));
  }
}