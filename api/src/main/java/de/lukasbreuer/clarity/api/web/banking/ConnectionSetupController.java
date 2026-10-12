package de.lukasbreuer.clarity.api.web.banking;

import de.lukasbreuer.clarity.api.banking.BankingConfiguration;
import de.lukasbreuer.clarity.api.banking.EnableBankingClient;
import de.lukasbreuer.clarity.api.banking.EnableBankingSigner;
import de.lukasbreuer.clarity.api.banking.connection.PendingAuthorization;
import de.lukasbreuer.clarity.api.banking.connection.PendingAuthorizationRepository;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Starts a bank login: asks Enable Banking for the bank's login page and
 * remembers the random state that has to come back with the redirect.
 * Error codes: 2000 Enable Banking is not set up or refused, 2003 invalid
 * bank or country.
 */
@RestController
@RequiredArgsConstructor
public final class ConnectionSetupController {
  private static final long MINIMUM_VALIDITY = 60L * 60 * 24;
  private static final long MAXIMUM_VALIDITY = 60L * 60 * 24 * 180;

  private final EnableBankingClient client;
  private final EnableBankingSigner signer;
  private final BankingConfiguration configuration;
  private final PendingAuthorizationRepository authorizationRepository;
  private final SecureRandom random = new SecureRandom();

  @RequestMapping(path = "/connections/start/", method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> start(
    @RequestBody String payload, HttpServletResponse response
  ) {
    if (!signer.isConfigured()) {
      return ApiResponse.error(2000).future();
    }
    var body = ApiRequestBody.of(payload, response);
    var bank = body.getString("bank", 200);
    var country = body.getString("country", 2);
    if (bank.isBlank() || !country.matches("[A-Z]{2}")) {
      return ApiResponse.error(2003).future();
    }
    var authorization = PendingAuthorization.create(generateState(), bank,
      country, System.currentTimeMillis());
    var validity = Math.clamp(body.getLong("maximum_consent_validity"),
      MINIMUM_VALIDITY, MAXIMUM_VALIDITY);
    return client.post("/auth", authorizationRequest(authorization, validity))
      .thenCompose(answer -> authorizationRepository.save(authorization)
        .thenApply(_ -> ApiResponse.success(Map.of("url",
          answer.getString("url")))))
      .exceptionally(_ -> ApiResponse.error(2000));
  }

  private JSONObject authorizationRequest(
    PendingAuthorization authorization, long validitySeconds
  ) {
    var validUntil = Instant.now().plusSeconds(validitySeconds);
    return new JSONObject()
      .put("access", new JSONObject().put("valid_until", validUntil.toString()))
      .put("aspsp", new JSONObject().put("name", authorization.bankName())
        .put("country", authorization.bankCountry()))
      .put("state", authorization.state())
      .put("redirect_url", configuration.redirectUrl())
      .put("psu_type", "personal");
  }

  private String generateState() {
    var bytes = new byte[32];
    random.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}