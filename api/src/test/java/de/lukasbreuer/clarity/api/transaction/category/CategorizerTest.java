package de.lukasbreuer.clarity.api.transaction.category;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class CategorizerTest {
  private static final CategoryKeywords KEYWORDS =
    CategoryKeywords.create(DefaultCategories.load().all());
  private static final BigDecimal DEBIT = new BigDecimal("-12.40");
  private static final BigDecimal CREDIT = new BigDecimal("2500.00");

  @Test
  void matchesBuiltInMerchantKeywords() {
    var categorizer = Categorizer.create(KEYWORDS, List.of(), List.of(), false);
    assertEquals("GROCERIES", categorizer.categorize("REWE Markt GmbH",
      null, "", DEBIT));
    assertEquals(Category.SUBSCRIPTIONS, categorizer.categorize(
      "Amazon Prime", null, "", DEBIT));
    assertEquals("SHOPPING", categorizer.categorize("AMAZON EU S.A R.L.",
      null, "", DEBIT));
  }

  @Test
  void keywordsOnlyMatchAtTheStartOfAWord() {
    var categorizer = Categorizer.create(KEYWORDS, List.of(), List.of(), false);
    assertEquals(Category.OTHER, categorizer.categorize("Tierarzt Dr. Meier",
      null, "", DEBIT));
  }

  @Test
  void ownRulesWinOverBuiltInKeywords() {
    var rule = CategoryRule.forCounterparty("  REWE   Markt ", "DINING");
    var categorizer = Categorizer.create(KEYWORDS, List.of(rule), List.of(),
      false);
    assertEquals("DINING", categorizer.categorize("REWE Markt GmbH",
      null, "", DEBIT));
  }

  @Test
  void movesBetweenOwnAccountsAreTransfers() {
    var categorizer = Categorizer.create(KEYWORDS, List.of(),
      List.of("DE89 3704 0044 0532 0130 00"), true);
    assertEquals(Category.TRANSFER, categorizer.categorize("Me",
      "de89370400440532013000", "", DEBIT));
    assertEquals(Category.TRANSFER, categorizer.categorize(
      "PayPal Europe S.a.r.l.", null, "Spotify", DEBIT));
  }

  @Test
  void payPalIsSpendingWhenPayPalIsNotConnected() {
    var categorizer = Categorizer.create(KEYWORDS, List.of(), List.of(), false);
    assertEquals(Category.SUBSCRIPTIONS, categorizer.categorize(
      "PayPal Europe S.a.r.l.", null, "Spotify AB", DEBIT));
  }

  @Test
  void unknownMoneyInIsIncome() {
    var categorizer = Categorizer.create(KEYWORDS, List.of(), List.of(), false);
    assertEquals(Category.INCOME, categorizer.categorize("Max Muster", null,
      "Danke fürs Essen", CREDIT));
    assertEquals(Category.SALARY, categorizer.categorize("Firma GmbH", null,
      "Gehalt Oktober", CREDIT));
  }

  @Test
  void ownCategoriesWinOverBuiltInKeywords() {
    var books = Category.create("books", "Bücher", "book-open", "gray",
      CategoryKind.SPENDING, List.of("amazon"), false, false, 19);
    var categories = new ArrayList<>(DefaultCategories.load().all());
    categories.add(books);
    var categorizer = Categorizer.create(CategoryKeywords.create(categories),
      List.of(), List.of(), false);
    assertEquals("books", categorizer.categorize("AMAZON EU S.A R.L.", null,
      "", DEBIT));
  }

  @Test
  void hiddenCategoriesTakeNoPart() {
    var categories = DefaultCategories.load().all();
    categories.stream().filter(category -> category.key().equals("GROCERIES"))
      .forEach(category -> category.hide(true));
    var categorizer = Categorizer.create(CategoryKeywords.create(categories),
      List.of(), List.of(), false);
    assertEquals(Category.OTHER, categorizer.categorize("REWE Markt GmbH",
      null, "", DEBIT));
  }
}