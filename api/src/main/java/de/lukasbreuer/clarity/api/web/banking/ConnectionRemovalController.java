package de.lukasbreuer.clarity.api.web.banking;

import de.lukasbreuer.clarity.api.banking.EnableBankingClient;
import de.lukasbreuer.clarity.api.banking.account.BankAccount;
import de.lukasbreuer.clarity.api.banking.account.BankAccountRepository;
import de.lukasbreuer.clarity.api.banking.connection.BankConnection;
import de.lukasbreuer.clarity.api.banking.connection.BankConnectionRepository;
import de.lukasbreuer.clarity.api.transaction.TransactionRepository;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Removes a connection completely: revokes the session at Enable Banking (so
 * the bank consent ends too), then deletes its accounts and every stored
 * transaction of them. Error code 2002: unknown connection.
 */
@RestController
@RequiredArgsConstructor
public final class ConnectionRemovalController {
  private final EnableBankingClient client;
  private final BankConnectionRepository connectionRepository;
  private final BankAccountRepository accountRepository;
  private final TransactionRepository transactionRepository;

  @RequestMapping(path = "/connections/delete/", method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> delete(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var connectionId = ApiRequestBody.of(payload, response).getUUID("id");
    if (connectionId == null) {
      return ApiResponse.error(2002).future();
    }
    return connectionRepository.findById(connectionId)
      .thenCompose(this::delete);
  }

  private CompletableFuture<ApiResponse> delete(
    Optional<BankConnection> connection
  ) {
    if (connection.isEmpty()) {
      return ApiResponse.error(2002).future();
    }
    return revoke(connection.get())
      .thenCompose(_ -> accountRepository.findByConnectionId(
        connection.get().id()))
      .thenCompose(this::deleteAccounts)
      .thenCompose(_ -> connectionRepository.delete(connection.get()))
      .thenApply(_ -> ApiResponse.success());
  }

  /**
   * A session that already expired at the bank cannot be revoked anymore,
   * which is fine: the local data goes either way.
   */
  private CompletableFuture<Void> revoke(BankConnection connection) {
    return client.delete("/sessions/" + connection.sessionId())
      .handle((_, _) -> null);
  }

  private CompletableFuture<Void> deleteAccounts(List<BankAccount> accounts) {
    accounts.forEach(account -> transactionRepository.deleteByAccountId(
      account.id()));
    return CompletableFuture.allOf(accounts.stream()
      .map(accountRepository::delete)
      .toArray(CompletableFuture[]::new));
  }
}