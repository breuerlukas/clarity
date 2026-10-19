package de.lukasbreuer.clarity.api.web.receipt;

import de.lukasbreuer.clarity.api.transaction.receipt.Receipt;
import de.lukasbreuer.clarity.api.transaction.receipt.ReceiptItem;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * The fields of a receipt as the owner corrected them, checked: a date and
 * a total may be empty while unknown, a total is above zero, and every
 * item has a name and an amount with at most cents.
 */
record ReceiptDraft(
  String merchant, LocalDate date, BigDecimal total, List<ReceiptItem> items
) {
  private static final int MAXIMUM_ITEMS = 100;
  private static final BigDecimal MAXIMUM_AMOUNT = new BigDecimal("1000000");

  static Optional<ReceiptDraft> parse(ApiRequestBody body) {
    try {
      var date = body.getString("date", 10);
      var total = body.getString("total", 24).trim();
      return Optional.of(new ReceiptDraft(
          body.getString("merchant", 120).trim(),
          date.isEmpty() ? null : LocalDate.parse(date),
          total.isEmpty() ? null : new BigDecimal(total),
          body.getBodyList("items").stream().map(ReceiptDraft::item).toList()))
        .filter(ReceiptDraft::isValid);
    } catch (RuntimeException exception) {
      return Optional.empty();
    }
  }

  private static ReceiptItem item(ApiRequestBody item) {
    return new ReceiptItem(item.getString("name", 120).trim(),
      new BigDecimal(item.getString("amount", 24)));
  }

  private boolean isValid() {
    return (total == null || (total.signum() > 0 && isAmount(total))) &&
      items.size() <= MAXIMUM_ITEMS && items.stream().allMatch(this::isItem);
  }

  private boolean isItem(ReceiptItem item) {
    return !item.name().isEmpty() && isAmount(item.amount());
  }

  private boolean isAmount(BigDecimal amount) {
    return amount.stripTrailingZeros().scale() <= 2 &&
      amount.abs().compareTo(MAXIMUM_AMOUNT) < 0;
  }

  void applyTo(Receipt receipt) {
    receipt.edit(merchant, date, total, items);
  }
}