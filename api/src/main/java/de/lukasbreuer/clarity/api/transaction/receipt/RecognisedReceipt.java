package de.lukasbreuer.clarity.api.transaction.receipt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * What could be read from a receipt; the owner reviews all of it. The date
 * and the total are null when none was found, the merchant is then empty.
 */
public record RecognisedReceipt(
  String merchant, LocalDate date, BigDecimal total, List<ReceiptItem> items
) {
  public static RecognisedReceipt nothing() {
    return new RecognisedReceipt("", null, null, List.of());
  }
}