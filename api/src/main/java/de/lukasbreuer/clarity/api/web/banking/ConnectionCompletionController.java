package de.lukasbreuer.clarity.api.web.banking;

import de.lukasbreuer.clarity.api.banking.EnableBankingClient;
import de.lukasbreuer.clarity.api.banking.account.BankAccountRepository;
import de.lukasbreuer.clarity.api.banking.connection.BankConnectionRepository;
import de.lukasbreuer.clarity.api.banking.connection.EnableBankingSession;
import de.lukasbreuer.clarity.api.banking.connection.PendingAuthorization;
import de.lukasbreuer.clarity.api.banking.connection.PendingAuthorizationRepository;
import de.lukasbreuer.clarity.api.banking.sync.SyncCoordinator;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Finishes a bank login once the bank redirected back with a code. The state
 * must be one this instance issued in the last 30 minutes and is burned on
 * first use. Error codes: 2000 Enable Banking refused the code, 2001 unknown
 * or expired state.
 */
@RestController
@RequiredArgsConstructor
public final class ConnectionCompletionController {
  private final EnableBankingClient client;
  private final PendingAuthorizationRepository authorizationRepository;
  private final BankConnectionRepository connectionRepository;
  private final BankAccountRepository accountRepository;
  private final SyncCoordinator syncCoordinator;

  @RequestMapping(path = "/connections/complete/", method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> complete(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var code = body.getString("code", 2000);
    return authorizationRepository.findById(body.getString("state", 100))
      .thenCompose(authorization -> complete(code, authorization));
  }

  private CompletableFuture<ApiResponse> complete(
    String code, Optional<PendingAuthorization> authorization
  ) {
    if (authorization.isEmpty()) {
      return ApiResponse.error(2001).future();
    }
    return authorizationRepository.delete(authorization.get())
      .thenCompose(_ -> authorization.get().isExpired() ?
        ApiResponse.error(2001).future() :
        createSession(code, authorization.get()));
  }

  private CompletableFuture<ApiResponse> createSession(
    String code, PendingAuthorization authorization
  ) {
    return client.post("/sessions", new JSONObject().put("code", code))
      .thenCompose(session -> store(EnableBankingSession.of(session,
        authorization)))
      .exceptionally(_ -> ApiResponse.error(2000));
  }

  private CompletableFuture<ApiResponse> store(EnableBankingSession session) {
    var connectionId = UUID.randomUUID();
    return connectionRepository.save(session.connection(connectionId))
      .thenCompose(_ -> CompletableFuture.allOf(session.accounts(connectionId)
        .stream().map(accountRepository::save)
        .toArray(CompletableFuture[]::new)))
      .thenApply(_ -> connected(connectionId));
  }

  private ApiResponse connected(UUID connectionId) {
    syncCoordinator.synchronizeNow(Map.of());
    return ApiResponse.success(Map.of("connection", connectionId));
  }
}