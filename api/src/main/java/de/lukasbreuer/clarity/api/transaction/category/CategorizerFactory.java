package de.lukasbreuer.clarity.api.transaction.category;

import de.lukasbreuer.clarity.api.banking.account.BankAccount;
import de.lukasbreuer.clarity.api.banking.account.BankAccountRepository;
import de.lukasbreuer.clarity.api.banking.connection.BankConnection;
import de.lukasbreuer.clarity.api.banking.connection.BankConnectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Builds a {@link Categorizer} from the current state: the owner's rules, the
 * IBANs of every connected account, and whether PayPal is among the banks.
 */
@Component
@RequiredArgsConstructor
public final class CategorizerFactory {
  private final CategoryRuleRepository ruleRepository;
  private final BankAccountRepository accountRepository;
  private final BankConnectionRepository connectionRepository;

  public CompletableFuture<Categorizer> create() {
    return ruleRepository.findAll()
      .thenCompose(rules -> accountRepository.findAll()
        .thenCompose(accounts -> connectionRepository.findAll()
          .thenApply(connections -> create(rules, accounts, connections))));
  }

  private Categorizer create(
    List<CategoryRule> rules, List<BankAccount> accounts,
    List<BankConnection> connections
  ) {
    var ownIbans = accounts.stream().map(BankAccount::iban)
      .filter(Objects::nonNull).toList();
    var paypalConnected = connections.stream().anyMatch(connection ->
      KeywordMatch.of(connection.bankName(), "paypal").matches());
    return Categorizer.create(rules, ownIbans, paypalConnected);
  }
}