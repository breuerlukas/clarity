package de.lukasbreuer.clarity.api.banking.sync;

import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Banks report several balances per account. The most useful one for "how
 * much do I have right now" is the available or booked balance of today;
 * older closing balances come last.
 */
@RequiredArgsConstructor(staticName = "of")
public final class BalanceSelection {
  private static final List<String> PREFERENCE = List.of("ITAV", "CLAV",
    "ITBD", "XPCD", "CLBD", "OPAV", "OPBD", "PRCD");

  private final JSONArray balances;

  public Optional<BigDecimal> select() {
    var candidates = IntStream.range(0, balances.length())
      .mapToObj(balances::getJSONObject).toList();
    return PREFERENCE.stream()
      .flatMap(type -> candidates.stream()
        .filter(balance -> type.equals(balance.optString("balance_type"))))
      .findFirst()
      .or(() -> candidates.stream().findFirst())
      .map(this::amount);
  }

  private BigDecimal amount(JSONObject balance) {
    return new BigDecimal(balance.getJSONObject("balance_amount")
      .getString("amount"));
  }
}