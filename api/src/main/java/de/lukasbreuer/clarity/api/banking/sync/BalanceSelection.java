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
 * older closing balances come last. Its currency also stands in for an
 * account that reports none of its own.
 */
@RequiredArgsConstructor(staticName = "of")
public final class BalanceSelection {
  private static final List<String> PREFERENCE = List.of("ITAV", "CLAV",
    "ITBD", "XPCD", "CLBD", "OPAV", "OPBD", "PRCD");

  private final JSONArray balances;

  public Optional<BigDecimal> select() {
    return selected().map(balance -> new BigDecimal(balance
      .getJSONObject("balance_amount").getString("amount")));
  }

  public Optional<String> currency() {
    return selected().map(balance -> balance.getJSONObject("balance_amount")
      .optString("currency", ""));
  }

  private Optional<JSONObject> selected() {
    var candidates = IntStream.range(0, balances.length())
      .mapToObj(balances::getJSONObject).toList();
    return PREFERENCE.stream()
      .flatMap(type -> candidates.stream()
        .filter(balance -> type.equals(balance.optString("balance_type"))))
      .findFirst()
      .or(() -> candidates.stream().findFirst());
  }
}