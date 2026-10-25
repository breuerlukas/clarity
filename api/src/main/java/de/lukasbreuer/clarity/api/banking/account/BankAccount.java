package de.lukasbreuer.clarity.api.banking.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * An account the owner shared during a bank connection, with the balance and
 * the outcome of the last sync.
 */
@Entity
@Table(name = "bank_account")
@Getter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(staticName = "create")
public final class BankAccount {
  private static final long SYNC_INTERVAL = 1000L * 60 * 60 * 6;
  private static final String NO_CURRENCY = "XXX";
  private static final List<String> SAVINGS_NAMES = List.of("tagesgeld",
    "spar", "saving", "depot", "festgeld", "anlage", "invest");

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;
  @Column(name = "connection_id", nullable = false)
  private UUID connectionId;
  @Column(name = "uid", nullable = false)
  private String uid;
  @Column(name = "iban")
  private String iban;
  @Column(name = "name")
  private String name;
  @Column(name = "currency", nullable = false)
  private String currency;
  @Column(name = "balance", precision = 19, scale = 4)
  private BigDecimal balance;
  @Column(name = "last_synced_at", nullable = false)
  private long lastSyncedAt;
  @Column(name = "last_attempt_at", nullable = false)
  private long lastAttemptAt;
  @Column(name = "sync_failure")
  private String syncFailure;
  @Column(name = "spending")
  private Boolean spending;

  /**
   * Whether the money on this account is meant for everyday spending and so
   * counts towards the room left until payday. Until the owner decides,
   * savings accounts are recognized by their name and left out.
   */
  public boolean isSpendingAccount() {
    if (spending != null) {
      return spending;
    }
    var lowerName = name == null ? "" : name.toLowerCase(Locale.ROOT);
    return SAVINGS_NAMES.stream().noneMatch(lowerName::contains);
  }

  public void markSpending(boolean spending) {
    this.spending = spending;
  }

  public void updateBalance(BigDecimal balance) {
    this.balance = balance;
  }

  /**
   * Multi-currency accounts such as PayPal report "XXX", the code for no
   * currency, for the account itself; the currency of its balance is the
   * one it is shown in.
   */
  public void adoptBalanceCurrency(String balanceCurrency) {
    if (NO_CURRENCY.equals(currency) && balanceCurrency.matches("[A-Z]{3}") &&
      !NO_CURRENCY.equals(balanceCurrency)) {
      this.currency = balanceCurrency;
    }
  }

  /**
   * PSD2 lets a bank refuse more than four reads a day without the owner
   * present, so the schedule reads each account at most every six hours,
   * counting failed attempts too.
   */
  public boolean isDueForSync() {
    return System.currentTimeMillis() - lastAttemptAt > SYNC_INTERVAL;
  }

  public void markSynced() {
    this.lastSyncedAt = System.currentTimeMillis();
    this.lastAttemptAt = lastSyncedAt;
    this.syncFailure = null;
  }

  /**
   * @param failure A short machine-readable reason such as "consent" or
   *                "status_500", never a response body
   */
  public void markFailed(String failure) {
    this.syncFailure = failure;
    this.lastAttemptAt = System.currentTimeMillis();
  }
}