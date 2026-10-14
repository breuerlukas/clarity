package de.lukasbreuer.clarity.api.web.space;

import de.lukasbreuer.clarity.api.banking.connection.BankConnectionRepository;
import de.lukasbreuer.clarity.api.business.contract.ContractRepository;
import de.lukasbreuer.clarity.api.business.invoice.InvoiceRepository;
import de.lukasbreuer.clarity.api.business.mail.MailboxRepository;
import de.lukasbreuer.clarity.api.space.Space;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Whether anything still belongs to a space: banks, contracts, mailboxes or
 * invoices. A space only goes once it holds none of them, so nothing is left
 * behind without a space to show it in.
 */
@Component
@RequiredArgsConstructor
final class SpaceContents {
  private final BankConnectionRepository connectionRepository;
  private final ContractRepository contractRepository;
  private final MailboxRepository mailboxRepository;
  private final InvoiceRepository invoiceRepository;

  CompletableFuture<Boolean> isEmpty(Space space) {
    return connectionRepository.findAll()
      .thenCompose(connections -> connections.stream()
        .anyMatch(connection -> space.id().equals(connection.spaceId())) ?
        CompletableFuture.completedFuture(false) :
        hasBusinessData(space.id()).thenApply(hasData -> !hasData));
  }

  private CompletableFuture<Boolean> hasBusinessData(UUID spaceId) {
    return contractRepository.existsBySpaceId(spaceId)
      .thenCombine(mailboxRepository.existsBySpaceId(spaceId),
        Boolean::logicalOr)
      .thenCombine(invoiceRepository.existsBySpaceId(spaceId),
        Boolean::logicalOr);
  }
}