package de.lukasbreuer.clarity.api.web.banking;

import de.lukasbreuer.clarity.api.banking.account.BankAccount;
import de.lukasbreuer.clarity.api.banking.account.BankAccountRepository;
import de.lukasbreuer.clarity.api.banking.connection.BankConnection;
import de.lukasbreuer.clarity.api.banking.connection.BankConnectionRepository;
import de.lukasbreuer.clarity.api.space.SpaceRepository;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Moves one account to another space; its transactions, fixed costs and
 * statistics go with it, as every view asks for the accounts of a space.
 * The bank connection stays where it was. Error codes: 2004 unknown
 * account, 5001 unknown space.
 */
@RestController
@RequiredArgsConstructor
public final class AccountMoveController {
  private final BankAccountRepository accountRepository;
  private final BankConnectionRepository connectionRepository;
  private final SpaceRepository spaceRepository;

  @RequestMapping(path = "/accounts/space/", method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> move(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var accountId = body.getUUID("id");
    var spaceId = body.getUUID("space");
    if (accountId == null) {
      return ApiResponse.error(2004).future();
    }
    if (spaceId == null) {
      return ApiResponse.error(5001).future();
    }
    return spaceRepository.existsById(spaceId)
      .thenCompose(exists -> exists ? accountRepository.findById(accountId)
        .thenCompose(account -> move(account, spaceId)) :
        ApiResponse.error(5001).future());
  }

  private CompletableFuture<ApiResponse> move(
    Optional<BankAccount> account, UUID spaceId
  ) {
    if (account.isEmpty()) {
      return ApiResponse.error(2004).future();
    }
    return connectionRepository.findById(account.get().connectionId())
      .thenCompose(connection -> connection
        .map(found -> save(account.get(), spaceId, found))
        .orElse(ApiResponse.error(2004).future()));
  }

  private CompletableFuture<ApiResponse> save(
    BankAccount account, UUID spaceId, BankConnection connection
  ) {
    account.moveTo(spaceId, connection);
    return accountRepository.save(account)
      .thenApply(_ -> ApiResponse.success());
  }
}
