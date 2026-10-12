package de.lukasbreuer.clarity.api.banking.sync;

import com.google.common.collect.Lists;
import de.lukasbreuer.clarity.api.banking.EnableBankingClient;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

/**
 * Reads every page of an account's transactions since a date by following the
 * continuation key. Stops after 100 pages in case a bank keeps handing out
 * keys forever.
 */
@RequiredArgsConstructor(staticName = "create")
public final class TransactionHistory {
  private static final int MAXIMUM_PAGES = 100;

  private final EnableBankingClient client;
  private final Map<String, String> headers;

  public CompletableFuture<List<JSONObject>> fetch(
    String accountUid, LocalDate from
  ) {
    return fetchPage(accountUid, from, "", Lists.newArrayList(), 1);
  }

  private CompletableFuture<List<JSONObject>> fetchPage(
    String accountUid, LocalDate from, String continuationKey,
    List<JSONObject> collected, int page
  ) {
    var path = "/accounts/" + encode(accountUid) + "/transactions?date_from=" +
      from + (continuationKey.isBlank() ? "" : "&continuation_key=" +
      encode(continuationKey));
    return client.get(path, headers).thenCompose(response ->
      collect(accountUid, from, response, collected, page));
  }

  private CompletableFuture<List<JSONObject>> collect(
    String accountUid, LocalDate from, JSONObject response,
    List<JSONObject> collected, int page
  ) {
    var transactions = response.optJSONArray("transactions", new JSONArray());
    IntStream.range(0, transactions.length())
      .mapToObj(transactions::getJSONObject)
      .forEach(collected::add);
    var continuationKey = response.optString("continuation_key", "");
    if (continuationKey.isBlank() || page >= MAXIMUM_PAGES) {
      return CompletableFuture.completedFuture(collected);
    }
    return fetchPage(accountUid, from, continuationKey, collected, page + 1);
  }

  private String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}