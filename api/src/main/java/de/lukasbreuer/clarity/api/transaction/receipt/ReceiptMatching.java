package de.lukasbreuer.clarity.api.transaction.receipt;

import de.lukasbreuer.clarity.api.transaction.Transaction;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The card payments a receipt may belong to: money out of a bank account
 * for exactly the receipt's total, booked up to a few days after the
 * purchase (or one day before, for a receipt dated by hand), and without a
 * receipt yet. The closest booking comes first.
 */
@RequiredArgsConstructor(staticName = "of")
public final class ReceiptMatching {
  private static final int MAXIMUM_DAYS_AFTER = 5;
  private static final int MAXIMUM_DAYS_BEFORE = 1;
  private static final int MAXIMUM_SUGGESTIONS = 5;

  private final Receipt receipt;

  /**
   * The first booking day worth loading; only for a receipt with a date.
   */
  public LocalDate earliestBooking() {
    return receipt.purchaseDate().minusDays(MAXIMUM_DAYS_BEFORE);
  }

  public LocalDate latestBooking() {
    return receipt.purchaseDate().plusDays(MAXIMUM_DAYS_AFTER);
  }

  /**
   * @param walletId The space's cash wallet, whose entries are no card
   *                 payments
   * @param withReceipt Transactions that have a receipt already
   */
  public List<Transaction> suggestions(
    List<Transaction> transactions, UUID walletId, Set<UUID> withReceipt
  ) {
    if (!receipt.hasDateAndTotal()) {
      return List.of();
    }
    return transactions.stream()
      .filter(transaction -> !transaction.accountId().equals(walletId))
      .filter(transaction -> !withReceipt.contains(transaction.id()))
      .filter(this::fits)
      .sorted(Comparator.comparingLong(this::distance))
      .limit(MAXIMUM_SUGGESTIONS)
      .toList();
  }

  private boolean fits(Transaction transaction) {
    var days = daysAfterPurchase(transaction);
    return transaction.amount().negate().compareTo(receipt.total()) == 0 &&
      days >= -MAXIMUM_DAYS_BEFORE && days <= MAXIMUM_DAYS_AFTER;
  }

  private long distance(Transaction transaction) {
    return Math.abs(daysAfterPurchase(transaction));
  }

  private long daysAfterPurchase(Transaction transaction) {
    return ChronoUnit.DAYS.between(receipt.purchaseDate(),
      transaction.bookingDate());
  }
}