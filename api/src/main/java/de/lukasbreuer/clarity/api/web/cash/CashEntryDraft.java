package de.lukasbreuer.clarity.api.web.cash;

import de.lukasbreuer.clarity.api.transaction.Transaction;
import de.lukasbreuer.clarity.api.transaction.cash.CashWallet;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * A cash entry as the panel sends it, checked: a date, an amount other than
 * zero with at most cents (negative for money spent), a category and an
 * optional counterparty and note.
 */
record CashEntryDraft(
  LocalDate date, BigDecimal amount, String counterparty, String note,
  String category
) {
  private static final BigDecimal MAXIMUM_AMOUNT = new BigDecimal("1000000000");

  static Optional<CashEntryDraft> parse(ApiRequestBody body) {
    try {
      var draft = new CashEntryDraft(
        LocalDate.parse(body.getString("date", 10)),
        new BigDecimal(body.getString("amount", 24)),
        body.getString("counterparty", 120).trim(),
        body.getString("note", 500).trim(), body.getString("category", 64));
      return Optional.of(draft).filter(CashEntryDraft::isValid);
    } catch (Exception exception) {
      return Optional.empty();
    }
  }

  private boolean isValid() {
    return amount.signum() != 0 && amount.stripTrailingZeros().scale() <= 2 &&
      amount.abs().compareTo(MAXIMUM_AMOUNT) < 0;
  }

  Transaction create(CashWallet wallet) {
    return wallet.entry(date, amount, counterparty, note, category);
  }

  void applyTo(Transaction entry) {
    entry.edit(date, amount, counterparty, note, category);
  }
}