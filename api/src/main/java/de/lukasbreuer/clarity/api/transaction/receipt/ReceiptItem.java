package de.lukasbreuer.clarity.api.transaction.receipt;

import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.Map;

/**
 * One line of a receipt: what was bought and what it cost, negative for a
 * deposit returned or a discount.
 */
public record ReceiptItem(String name, BigDecimal amount) {
  public static ReceiptItem of(JSONObject item) {
    return new ReceiptItem(item.getString("name"),
      item.getBigDecimal("amount"));
  }

  public Map<String, Object> information() {
    return Map.of("name", name, "amount", amount);
  }
}