package de.lukasbreuer.clarity.api.transaction.category;

import de.lukasbreuer.clarity.api.transaction.Transaction;
import de.lukasbreuer.clarity.api.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Saves a rule and applies it right away to every stored transaction from
 * that counterparty, except the ones the owner categorized by hand.
 */
@Component
@RequiredArgsConstructor
public final class CategoryRuleApplication {
  private final CategoryRuleRepository ruleRepository;
  private final TransactionRepository transactionRepository;

  public CompletableFuture<Integer> apply(CategoryRule rule) {
    return ruleRepository.save(rule)
      .thenCompose(_ -> transactionRepository.findByManuallyCategorized(false))
      .thenCompose(transactions -> recategorize(rule, transactions));
  }

  private CompletableFuture<Integer> recategorize(
    CategoryRule rule, List<Transaction> transactions
  ) {
    var matching = transactions.stream()
      .filter(transaction -> transaction.category() != rule.category())
      .filter(transaction -> KeywordMatch.of(transaction.counterparty(),
        rule.keyword()).matches())
      .toList();
    matching.forEach(transaction -> transaction.categorize(rule.category()));
    return transactionRepository.saveAll(matching).thenApply(List::size);
  }
}