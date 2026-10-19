package de.lukasbreuer.clarity.api.web.transaction;

import de.lukasbreuer.clarity.api.transaction.split.TransactionSplit;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * One part of a split as the panel sends it: a category and an amount with
 * the sign of the transaction.
 */
record SplitPart(String category, BigDecimal amount) {
  private static final int MAXIMUM_PARTS = 50;

  /**
   * @return Empty if any part does not parse or there are too many
   */
  static Optional<List<SplitPart>> parseAll(List<ApiRequestBody> parts) {
    var parsed = parts.stream().map(SplitPart::parse)
      .flatMap(Optional::stream).toList();
    return parsed.size() == parts.size() && parsed.size() <= MAXIMUM_PARTS ?
      Optional.of(parsed) : Optional.empty();
  }

  static Optional<SplitPart> parse(ApiRequestBody part) {
    try {
      return Optional.of(new SplitPart(part.getString("category", 64),
        new BigDecimal(part.getString("amount", 24))));
    } catch (NumberFormatException exception) {
      return Optional.empty();
    }
  }

  TransactionSplit create(UUID transactionId) {
    return TransactionSplit.create(UUID.randomUUID(), transactionId, category,
      amount);
  }
}