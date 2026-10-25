package de.lukasbreuer.clarity.api.web.space;

import de.lukasbreuer.clarity.api.banking.connection.BankConnectionRepository;
import de.lukasbreuer.clarity.api.business.contract.ContractRepository;
import de.lukasbreuer.clarity.api.business.invoice.InvoiceRepository;
import de.lukasbreuer.clarity.api.business.mail.MailboxRepository;
import de.lukasbreuer.clarity.api.space.Space;
import de.lukasbreuer.clarity.api.space.goal.SavingsGoalRepository;
import de.lukasbreuer.clarity.api.transaction.TransactionRepository;
import de.lukasbreuer.clarity.api.transaction.cash.CashWalletRepository;
import de.lukasbreuer.clarity.api.transaction.receipt.ReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Whether anything still belongs to a space: banks, cash entries, receipts,
 * savings goals, contracts, mailboxes or invoices. A space only goes once
 * it holds none of them, so nothing is left behind without a space to show
 * it in. An empty cash wallet does not count; it stays behind unused.
 */
@Component
@RequiredArgsConstructor
final class SpaceContents {
  private final BankConnectionRepository connectionRepository;
  private final ContractRepository contractRepository;
  private final MailboxRepository mailboxRepository;
  private final InvoiceRepository invoiceRepository;
  private final CashWalletRepository walletRepository;
  private final TransactionRepository transactionRepository;
  private final ReceiptRepository receiptRepository;
  private final SavingsGoalRepository goalRepository;

  CompletableFuture<Boolean> isEmpty(Space space) {
    return connectionRepository.findAll()
      .thenCompose(connections -> connections.stream()
        .anyMatch(connection -> space.id().equals(connection.spaceId())) ?
        CompletableFuture.completedFuture(false) :
        hasData(space.id()).thenApply(hasData -> !hasData));
  }

  private CompletableFuture<Boolean> hasData(UUID spaceId) {
    return hasBusinessData(spaceId)
      .thenCombine(hasCashEntries(spaceId), Boolean::logicalOr)
      .thenCombine(receiptRepository.existsBySpaceId(spaceId),
        Boolean::logicalOr)
      .thenCombine(goalRepository.existsBySpaceId(spaceId),
        Boolean::logicalOr);
  }

  private CompletableFuture<Boolean> hasCashEntries(UUID spaceId) {
    return walletRepository.findBySpaceId(spaceId)
      .thenCompose(wallet -> wallet.isEmpty() ?
        CompletableFuture.completedFuture(false) :
        transactionRepository.existsByAccountId(wallet.get().id()));
  }

  private CompletableFuture<Boolean> hasBusinessData(UUID spaceId) {
    return contractRepository.existsBySpaceId(spaceId)
      .thenCombine(mailboxRepository.existsBySpaceId(spaceId),
        Boolean::logicalOr)
      .thenCombine(invoiceRepository.existsBySpaceId(spaceId),
        Boolean::logicalOr);
  }
}