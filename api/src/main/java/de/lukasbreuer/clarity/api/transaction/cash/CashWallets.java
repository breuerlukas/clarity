package de.lukasbreuer.clarity.api.transaction.cash;

import de.lukasbreuer.clarity.api.transaction.Transaction;
import de.lukasbreuer.clarity.api.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Hands out the wallet of a space, starting it the first time it is asked
 * for, and the entries booked on a wallet. When two requests start a wallet
 * at once, the unique space id lets one save fail, and that one reads the
 * wallet the other saved.
 */
@Component
@RequiredArgsConstructor
public final class CashWallets {
  private final CashWalletRepository walletRepository;
  private final TransactionRepository transactionRepository;

  /**
   * @param spaceId A space that exists; the caller checks
   */
  public CompletableFuture<CashWallet> of(UUID spaceId) {
    return walletRepository.findBySpaceId(spaceId)
      .thenCompose(wallet -> wallet.map(CompletableFuture::completedFuture)
        .orElseGet(() -> start(spaceId)));
  }

  /**
   * @return Empty for an unknown transaction and for a bank transaction,
   *         which stays as the bank reported it
   */
  public CompletableFuture<Optional<Transaction>> entry(UUID transactionId) {
    return transactionRepository.findById(transactionId)
      .thenCompose(transaction -> transaction.isEmpty() ?
        CompletableFuture.completedFuture(transaction) :
        walletRepository.existsById(transaction.get().accountId())
          .thenApply(isWallet -> transaction.filter(_ -> isWallet)));
  }

  private CompletableFuture<CashWallet> start(UUID spaceId) {
    return walletRepository.save(CashWallet.create(UUID.randomUUID(),
        spaceId, LocalDate.now()))
      .exceptionallyCompose(_ -> walletRepository.findBySpaceId(spaceId)
        .thenApply(Optional::orElseThrow));
  }
}