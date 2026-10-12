package de.lukasbreuer.clarity.api.banking.sync;

import com.google.common.hash.Hashing;
import de.lukasbreuer.clarity.api.transaction.Transaction;
import de.lukasbreuer.clarity.api.transaction.category.Categorizer;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Reads one transaction as Enable Banking delivers it. Amounts arrive
 * unsigned; the credit/debit indicator decides the sign. The counterparty is
 * the creditor for money going out and the debtor for money coming in.
 */
@RequiredArgsConstructor(staticName = "of")
public final class EnableBankingTransaction {
  private static final int MAXIMUM_DESCRIPTION_LENGTH = 1000;

  private final JSONObject json;

  /**
   * Pending transactions are skipped: banks often book them with a different
   * reference and amount, which would leave a stale duplicate behind.
   */
  public boolean isBooked() {
    return json.optString("status", "BOOK").equals("BOOK");
  }

  public boolean isDebit() {
    return json.optString("credit_debit_indicator").equals("DBIT");
  }

  public BigDecimal amount() {
    var amount = new BigDecimal(json.getJSONObject("transaction_amount")
      .getString("amount")).abs();
    return isDebit() ? amount.negate() : amount;
  }

  public String currency() {
    return json.getJSONObject("transaction_amount").optString("currency", "EUR");
  }

  public LocalDate bookingDate() {
    var date = json.optString("booking_date", json.optString("value_date",
      json.optString("transaction_date")));
    return LocalDate.parse(date);
  }

  public String counterparty() {
    var party = json.optJSONObject(isDebit() ? "creditor" : "debtor");
    return party == null ? "" : party.optString("name", "").trim();
  }

  public String counterpartyIban() {
    var account = json.optJSONObject(isDebit() ? "creditor_account" :
      "debtor_account");
    return account == null ? null : account.optString("iban", null);
  }

  public String description() {
    var lines = json.optJSONArray("remittance_information");
    if (lines == null) {
      return "";
    }
    var description = IntStream.range(0, lines.length())
      .mapToObj(lines::optString)
      .collect(Collectors.joining(" ")).trim();
    return description.substring(0, Math.min(description.length(),
      MAXIMUM_DESCRIPTION_LENGTH));
  }

  /**
   * The bank's entry reference when there is one. Some banks send none; then
   * a hash over the content stands in, stable across syncs as long as the bank
   * reports the transaction the same way.
   */
  public String externalId() {
    var reference = json.optString("entry_reference", "");
    if (!reference.isBlank()) {
      return reference;
    }
    var content = bookingDate() + "|" + amount().toPlainString() + "|" +
      counterparty() + "|" + description();
    return "hash:" + Hashing.sha256().hashString(content,
      StandardCharsets.UTF_8);
  }

  public Transaction toTransaction(UUID accountId, Categorizer categorizer) {
    var category = categorizer.categorize(counterparty(), counterpartyIban(),
      description(), amount());
    return Transaction.create(UUID.randomUUID(), accountId, externalId(),
      bookingDate(), amount(), currency(), counterparty(), counterpartyIban(),
      description(), category, false);
  }
}