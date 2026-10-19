package de.lukasbreuer.clarity.api.transaction.cash;

import de.lukasbreuer.clarity.api.banking.account.AccountScope;
import de.lukasbreuer.clarity.api.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

/**
 * Works out what should be in a wallet from every transaction of its space:
 * its own entries and the ATM withdrawals of the bank accounts.
 */
@Component
@RequiredArgsConstructor
public final class CashBalances {
  private final AccountScope accountScope;
  private final TransactionRepository transactionRepository;

  public CompletableFuture<BigDecimal> of(CashWallet wallet) {
    return accountScope.accountIds(wallet.spaceId())
      .thenCompose(transactionRepository::findByAccountIdIn)
      .thenApply(wallet::balance);
  }
}