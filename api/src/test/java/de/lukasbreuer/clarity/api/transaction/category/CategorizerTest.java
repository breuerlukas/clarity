package de.lukasbreuer.clarity.api.transaction.category;

import de.lukasbreuer.clarity.api.transaction.Category;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class CategorizerTest {
  private static final BigDecimal DEBIT = new BigDecimal("-12.40");
  private static final BigDecimal CREDIT = new BigDecimal("2500.00");

  @Test
  void matchesBuiltInMerchantKeywords() {
    var categorizer = Categorizer.create(List.of(), List.of(), false);
    assertEquals(Category.GROCERIES, categorizer.categorize("REWE Markt GmbH",
      null, "", DEBIT));
    assertEquals(Category.SUBSCRIPTIONS, categorizer.categorize(
      "Amazon Prime", null, "", DEBIT));
    assertEquals(Category.SHOPPING, categorizer.categorize("AMAZON EU S.A R.L.",
      null, "", DEBIT));
  }

  @Test
  void keywordsOnlyMatchAtTheStartOfAWord() {
    var categorizer = Categorizer.create(List.of(), List.of(), false);
    assertEquals(Category.OTHER, categorizer.categorize("Tierarzt Dr. Meier",
      null, "", DEBIT));
  }

  @Test
  void ownRulesWinOverBuiltInKeywords() {
    var rule = CategoryRule.forCounterparty("  REWE   Markt ", Category.DINING);
    var categorizer = Categorizer.create(List.of(rule), List.of(), false);
    assertEquals(Category.DINING, categorizer.categorize("REWE Markt GmbH",
      null, "", DEBIT));
  }

  @Test
  void movesBetweenOwnAccountsAreTransfers() {
    var categorizer = Categorizer.create(List.of(),
      List.of("DE89 3704 0044 0532 0130 00"), true);
    assertEquals(Category.TRANSFER, categorizer.categorize("Me",
      "de89370400440532013000", "", DEBIT));
    assertEquals(Category.TRANSFER, categorizer.categorize(
      "PayPal Europe S.a.r.l.", null, "Spotify", DEBIT));
  }

  @Test
  void payPalIsSpendingWhenPayPalIsNotConnected() {
    var categorizer = Categorizer.create(List.of(), List.of(), false);
    assertEquals(Category.SUBSCRIPTIONS, categorizer.categorize(
      "PayPal Europe S.a.r.l.", null, "Spotify AB", DEBIT));
  }

  @Test
  void unknownMoneyInIsIncome() {
    var categorizer = Categorizer.create(List.of(), List.of(), false);
    assertEquals(Category.INCOME, categorizer.categorize("Max Muster", null,
      "Danke fürs Essen", CREDIT));
    assertEquals(Category.SALARY, categorizer.categorize("Firma GmbH", null,
      "Gehalt Oktober", CREDIT));
  }
}