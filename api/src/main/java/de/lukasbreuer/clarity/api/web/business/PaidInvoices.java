package de.lukasbreuer.clarity.api.web.business;

import de.lukasbreuer.clarity.api.business.invoice.Invoice;
import de.lukasbreuer.clarity.api.business.invoice.InvoicePayment;
import de.lukasbreuer.clarity.api.business.invoice.InvoicePaymentRepository;
import de.lukasbreuer.clarity.api.business.invoice.InvoiceRepository;
import de.lukasbreuer.clarity.api.transaction.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * The invoices each booking of a business space pays, so the panel can show
 * them next to the booking. Rejected links do not count.
 */
@Component
@RequiredArgsConstructor
final class PaidInvoices {
  private final InvoiceRepository invoiceRepository;
  private final InvoicePaymentRepository paymentRepository;

  /**
   * @return Per transaction its invoices as id, number and amount paid
   */
  CompletableFuture<Map<UUID, List<Map<String, Object>>>> byTransaction(
    UUID spaceId, List<Transaction> transactions
  ) {
    if (transactions.isEmpty()) {
      return CompletableFuture.completedFuture(Map.of());
    }
    return invoiceRepository.findBySpaceId(spaceId)
      .thenCombine(paymentRepository.findByTransactionIdIn(transactions
        .stream().map(Transaction::id).toList()), this::group);
  }

  private Map<UUID, List<Map<String, Object>>> group(
    List<Invoice> invoices, List<InvoicePayment> payments
  ) {
    var numbers = invoices.stream()
      .collect(Collectors.toMap(Invoice::id, Invoice::number));
    return payments.stream()
      .filter(payment -> !payment.rejected())
      .filter(payment -> numbers.containsKey(payment.invoiceId()))
      .collect(Collectors.groupingBy(InvoicePayment::transactionId,
        Collectors.mapping(payment -> information(payment, numbers),
          Collectors.toList())));
  }

  private Map<String, Object> information(
    InvoicePayment payment, Map<UUID, String> numbers
  ) {
    return Map.of("id", payment.invoiceId(),
      "number", numbers.get(payment.invoiceId()),
      "amount", payment.amount());
  }
}