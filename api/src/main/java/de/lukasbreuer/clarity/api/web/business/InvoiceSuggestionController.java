package de.lukasbreuer.clarity.api.web.business;

import de.lukasbreuer.clarity.api.business.invoice.Invoice;
import de.lukasbreuer.clarity.api.business.invoice.InvoiceRepository;
import de.lukasbreuer.clarity.api.business.invoice.matching.InvoiceMatcher;
import de.lukasbreuer.clarity.api.business.invoice.matching.PaymentSuggestion;
import de.lukasbreuer.clarity.api.web.request.UuidParameter;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Bookings that may pay an invoice, best first, with why each fits, for
 * the owner to link by hand. Error codes: 7001 unknown invoice.
 */
@RestController
@RequiredArgsConstructor
public final class InvoiceSuggestionController {
  private final InvoiceRepository invoiceRepository;
  private final InvoiceMatcher invoiceMatcher;

  @RequestMapping(path = "/business/invoices/suggestions/",
    method = RequestMethod.GET)
  public CompletableFuture<ApiResponse> suggest(
    @RequestParam("id") String id
  ) {
    var invoiceId = UuidParameter.parse(id);
    if (invoiceId.isEmpty()) {
      return ApiResponse.error(7001).future();
    }
    return invoiceRepository.findById(invoiceId.get())
      .thenApply(invoice -> invoice.map(this::suggestions)
        .orElseGet(() -> ApiResponse.error(7001)));
  }

  private ApiResponse suggestions(Invoice invoice) {
    return ApiResponse.success(Map.of("suggestions",
      invoiceMatcher.suggest(invoice).stream()
        .map(this::information)
        .toList()));
  }

  private Map<String, Object> information(PaymentSuggestion suggestion) {
    return Map.of("transaction", suggestion.transaction().information(),
      "reasons", suggestion.reasons().stream().map(Enum::name).toList());
  }
}