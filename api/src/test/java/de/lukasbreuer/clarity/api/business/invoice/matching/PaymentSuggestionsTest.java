package de.lukasbreuer.clarity.api.business.invoice.matching;

import de.lukasbreuer.clarity.api.business.invoice.InvoiceFixtures;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PaymentSuggestionsTest {
  private final InvoiceFixtures fixtures = InvoiceFixtures.create();

  @Test
  void leavesAmbiguousPaymentsToTheOwner() {
    var first = fixtures.outgoing("RE-1", "Kunde AG", "500.00");
    var second = fixtures.outgoing("RE-2", "Kunde AG", "500.00");
    var transaction = fixtures.booking("500.00", "Kunde AG", "Danke");
    var matching = PaymentMatching.create(List.of(first, second), List.of());
    assertTrue(matching.match(List.of(transaction), Set.of()).links()
      .isEmpty());
    var suggestions = matching.suggest(first, List.of(transaction), Set.of());
    assertEquals(Set.of(MatchReason.AMOUNT, MatchReason.NAME),
      suggestions.getFirst().reasons());
  }

  @Test
  void suggestsNothingWithoutAReason() {
    var invoice = fixtures.outgoing("RE-3", "Kunde AG", "500.00");
    var transaction = fixtures.booking("20.00", "Jemand", "Danke");
    var suggestions = PaymentMatching.create(List.of(invoice), List.of())
      .suggest(invoice, List.of(transaction), Set.of());
    assertTrue(suggestions.isEmpty());
  }
}