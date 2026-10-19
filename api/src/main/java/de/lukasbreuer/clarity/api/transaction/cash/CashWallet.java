package de.lukasbreuer.clarity.api.transaction.cash;

import de.lukasbreuer.clarity.api.transaction.Transaction;
import de.lukasbreuer.clarity.api.transaction.category.Category;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The cash of one space, an account of its own next to the bank accounts.
 * Its entries are transactions booked on the wallet's id, which no bank sync
 * ever reads, stores or deletes. ATM withdrawals fill it from the day it
 * started; the cash of earlier ones is long spent, and counting the cash
 * corrects whatever is off.
 */
@Entity
@Table(name = "cash_wallet",
  uniqueConstraints = @UniqueConstraint(name = "uq_cash_wallet_space",
    columnNames = "space_id"))
@Getter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(staticName = "create")
public final class CashWallet {
  public static final String CURRENCY = "EUR";
  private static final String ENTRY_PREFIX = "cash-";

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;
  @Column(name = "space_id", nullable = false, updatable = false)
  private UUID spaceId;
  @Column(name = "started_on", nullable = false, updatable = false)
  private LocalDate startedOn;

  public boolean holds(Transaction transaction) {
    return id.equals(transaction.accountId());
  }

  /**
   * @param transactions Every transaction of the space, bank and cash
   */
  public BigDecimal balance(List<Transaction> transactions) {
    return transactions.stream().map(this::change)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * An entry the owner books by hand; the category counts as picked by hand
   * too, so no rule moves it later.
   */
  public Transaction entry(
    LocalDate date, BigDecimal amount, String counterparty, String note,
    String category
  ) {
    var entryId = UUID.randomUUID();
    return Transaction.create(entryId, id, ENTRY_PREFIX + entryId, date,
      amount, CURRENCY, counterparty, null, note, category, true);
  }

  public Map<String, Object> information(BigDecimal balance) {
    return Map.of("id", id,
      "space", spaceId,
      "balance", balance,
      "currency", CURRENCY,
      "started_on", startedOn.toString());
  }

  /**
   * A withdrawal moves money from a bank account into the wallet, so it
   * counts with the opposite sign; paying cash in at the bank takes it out
   * again.
   */
  private BigDecimal change(Transaction transaction) {
    if (holds(transaction)) {
      return transaction.amount();
    }
    return isWithdrawal(transaction) ? transaction.amount().negate() :
      BigDecimal.ZERO;
  }

  private boolean isWithdrawal(Transaction transaction) {
    return Category.CASH.equals(transaction.category()) &&
      !transaction.bookingDate().isBefore(startedOn);
  }
}