package de.lukasbreuer.clarity.api.web.banking;

import de.lukasbreuer.clarity.api.banking.EnableBankingClient;
import de.lukasbreuer.clarity.api.banking.EnableBankingSigner;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

/**
 * The banks of one country that offer account access for private customers.
 * Error codes: 2000 Enable Banking is not set up or not reachable, 2003
 * unknown country.
 */
@RestController
@RequiredArgsConstructor
public final class BankController {
  private final EnableBankingClient client;
  private final EnableBankingSigner signer;

  @RequestMapping(path = "/banks/", method = RequestMethod.GET)
  public CompletableFuture<ApiResponse> findBanks(
    @RequestParam("country") String country
  ) {
    if (!signer.isConfigured()) {
      return ApiResponse.error(2000).future();
    }
    if (!country.matches("[A-Z]{2}")) {
      return ApiResponse.error(2003).future();
    }
    return client.get("/aspsps?psu_type=personal&service=AIS&country=" +
        country)
      .thenApply(response -> ApiResponse.success(Map.of("banks",
        banks(response.optJSONArray("aspsps", new JSONArray())))))
      .exceptionally(_ -> ApiResponse.error(2000));
  }

  private Object banks(JSONArray banks) {
    return IntStream.range(0, banks.length())
      .mapToObj(banks::getJSONObject)
      .map(this::bank)
      .toList();
  }

  private Map<String, Object> bank(JSONObject bank) {
    return Map.of("name", bank.getString("name"),
      "country", bank.getString("country"),
      "maximum_consent_validity", bank.optLong("maximum_consent_validity",
        60L * 60 * 24 * 90));
  }
}