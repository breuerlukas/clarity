package de.lukasbreuer.clarity.api.banking.connection;

import de.lukasbreuer.clarity.api.banking.account.BankAccount;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Reads the session Enable Banking returns once the owner finished the bank
 * login: which accounts they shared, and how long the bank lets us read them.
 */
@RequiredArgsConstructor(staticName = "of")
public final class EnableBankingSession {
  private final JSONObject json;
  private final PendingAuthorization authorization;

  public BankConnection connection(UUID connectionId) {
    return BankConnection.create(connectionId, json.getString("session_id"),
      authorization.bankName(), authorization.bankCountry(), validUntil(),
      System.currentTimeMillis(), authorization.spaceId());
  }

  public UUID spaceId() {
    return authorization.spaceId();
  }

  public List<BankAccount> accounts(UUID connectionId) {
    var accounts = json.optJSONArray("accounts", new JSONArray());
    return IntStream.range(0, accounts.length())
      .mapToObj(accounts::getJSONObject)
      .map(account -> account(connectionId, account))
      .toList();
  }

  private BankAccount account(UUID connectionId, JSONObject account) {
    var accountId = account.optJSONObject("account_id", new JSONObject());
    var name = accountName(account);
    return BankAccount.create(UUID.randomUUID(), connectionId,
      account.getString("uid"), accountId.optString("iban", null), name,
      account.optString("currency", "EUR"), null, 0, 0, null, null, null, null);
  }

  /**
   * Banks often put the holder's name into "name"; the product ("Girokonto",
   * "Cuenta Open") says which account it is, so it wins when present.
   */
  private String accountName(JSONObject account) {
    return Stream.of("product", "details", "name")
      .map(key -> account.optString(key, "").trim())
      .filter(value -> !value.isEmpty())
      .findFirst()
      .orElse("");
  }

  private long validUntil() {
    var access = json.optJSONObject("access", new JSONObject());
    try {
      return OffsetDateTime.parse(access.getString("valid_until"))
        .toInstant().toEpochMilli();
    } catch (Exception exception) {
      return System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 90;
    }
  }
}