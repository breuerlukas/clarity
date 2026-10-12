package de.lukasbreuer.clarity.api.transaction.category;

import de.lukasbreuer.clarity.api.transaction.Category;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Picks a category for a transaction, first match wins:
 * money moved between the owner's own accounts (including the top-ups of a
 * connected PayPal account) is a transfer, then the owner's own rules, then
 * the built-in merchant keywords, and whatever is left is plain income or
 * "other".
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class Categorizer {
  public static Categorizer create(
    CategoryKeywords keywords, List<CategoryRule> rules,
    Collection<String> ownIbans, boolean paypalConnected
  ) {
    var normalizedIbans = ownIbans.stream()
      .map(iban -> iban.replaceAll("\\s", "").toUpperCase(Locale.ROOT))
      .collect(Collectors.toSet());
    return new Categorizer(keywords, rules, normalizedIbans, paypalConnected);
  }

  private final CategoryKeywords keywords;
  private final List<CategoryRule> rules;
  private final Set<String> ownIbans;
  private final boolean paypalConnected;

  public Category categorize(
    String counterparty, String counterpartyIban, String description,
    BigDecimal amount
  ) {
    if (isOwnTransfer(counterparty, counterpartyIban)) {
      return Category.TRANSFER;
    }
    var text = counterparty + " " + description;
    return rules.stream()
      .filter(rule -> KeywordMatch.of(counterparty, rule.keyword()).matches())
      .map(CategoryRule::category)
      .findFirst()
      .or(() -> keywords.find(text))
      .orElse(amount.signum() > 0 ? Category.INCOME : Category.OTHER);
  }

  private boolean isOwnTransfer(String counterparty, String counterpartyIban) {
    if (counterpartyIban != null && ownIbans.contains(
      counterpartyIban.replaceAll("\\s", "").toUpperCase(Locale.ROOT))) {
      return true;
    }
    return paypalConnected && KeywordMatch.of(counterparty, "paypal").matches();
  }
}