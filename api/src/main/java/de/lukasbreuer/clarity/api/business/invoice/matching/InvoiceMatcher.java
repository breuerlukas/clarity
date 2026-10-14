package de.lukasbreuer.clarity.api.business.invoice.matching;

import de.lukasbreuer.clarity.api.banking.account.AccountScope;
import de.lukasbreuer.clarity.api.business.invoice.Invoice;
import de.lukasbreuer.clarity.api.business.invoice.InvoicePayment;
import de.lukasbreuer.clarity.api.business.invoice.InvoicePaymentRepository;
import de.lukasbreuer.clarity.api.business.invoice.InvoiceRepository;
import de.lukasbreuer.clarity.api.business.payment.PaymentAssignment;
import de.lukasbreuer.clarity.api.business.payment.PaymentAssignmentRepository;
import de.lukasbreuer.clarity.api.business.payment.PaymentKind;
import de.lukasbreuer.clarity.api.space.Space;
import de.lukasbreuer.clarity.api.space.SpaceKind;
import de.lukasbreuer.clarity.api.space.SpaceRepository;
import de.lukasbreuer.clarity.api.transaction.Transaction;
import de.lukasbreuer.clarity.api.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Runs the payment matching for a business space after new mails or new
 * bookings arrived, one space at a time so two runs never link the same
 * booking twice.
 */
@Component
@RequiredArgsConstructor
public final class InvoiceMatcher {
  private static final int EARLY_PAYMENT_DAYS = 30;

  private final InvoiceRepository invoiceRepository;
  private final InvoicePaymentRepository paymentRepository;
  private final TransactionRepository transactionRepository;
  private final PaymentAssignmentRepository assignmentRepository;
  private final SpaceRepository spaceRepository;
  private final AccountScope accountScope;

  public void matchAll() {
    spaceRepository.findByKind(SpaceKind.BUSINESS).join().stream()
      .map(Space::id)
      .forEach(this::match);
  }

  public synchronized void match(UUID spaceId) {
    var invoices = invoiceRepository.findBySpaceId(spaceId).join();
    if (invoices.isEmpty()) {
      return;
    }
    var result = PaymentMatching.create(invoices, payments(invoices))
      .match(transactions(spaceId, invoices), excludedTransactions());
    store(result);
  }

  public synchronized List<PaymentSuggestion> suggest(Invoice invoice) {
    var invoices = List.of(invoice);
    return PaymentMatching.create(invoices, payments(invoices))
      .suggest(invoice, transactions(invoice.spaceId(), invoices),
        excludedTransactions());
  }

  /**
   * Every link of the given invoices, rejected ones included.
   */
  public List<InvoicePayment> payments(List<Invoice> invoices) {
    if (invoices.isEmpty()) {
      return List.of();
    }
    return paymentRepository.findByInvoiceIdIn(invoices.stream()
      .map(Invoice::id).toList()).join();
  }

  private void store(MatchResult result) {
    if (!result.links().isEmpty()) {
      paymentRepository.saveAll(result.links()).join();
    }
    if (!result.settled().isEmpty()) {
      invoiceRepository.saveAll(result.settled()).join();
    }
  }

  private List<Transaction> transactions(
    UUID spaceId, List<Invoice> invoices
  ) {
    var from = invoices.stream().map(Invoice::invoiceDate)
      .min(Comparator.naturalOrder()).orElseThrow()
      .minusDays(EARLY_PAYMENT_DAYS);
    var accountIds = accountScope.accountIds(spaceId).join();
    return transactionRepository.findByAccountsBetween(accountIds, from,
      LocalDate.now().plusDays(1)).join();
  }

  /**
   * Bookings the owner marked as private withdrawals or deposits.
   */
  private Set<UUID> excludedTransactions() {
    return assignmentRepository.findAll().join().stream()
      .filter(assignment -> assignment.kind() == PaymentKind.WITHDRAWAL ||
        assignment.kind() == PaymentKind.DEPOSIT)
      .map(PaymentAssignment::transactionId)
      .collect(Collectors.toSet());
  }
}