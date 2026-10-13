package de.lukasbreuer.clarity.api.web.banking;

import de.lukasbreuer.clarity.api.banking.account.BankAccount;
import de.lukasbreuer.clarity.api.banking.account.BankAccountRepository;
import de.lukasbreuer.clarity.api.banking.connection.BankConnection;
import de.lukasbreuer.clarity.api.banking.connection.BankConnectionRepository;
import de.lukasbreuer.clarity.api.banking.sync.SyncCoordinator;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import de.lukasbreuer.clarity.api.web.security.ClientAddress;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequiredArgsConstructor
public final class ConnectionController {
  private final BankConnectionRepository connectionRepository;
  private final BankAccountRepository accountRepository;
  private final SyncCoordinator syncCoordinator;

  /**
   * The connections of every space, each saying which one it belongs to; the
   * panel needs the other spaces' accounts to recognize private transfers.
   */
  @RequestMapping(path = "/connections/", method = RequestMethod.GET)
  public CompletableFuture<ApiResponse> findConnections() {
    return connectionRepository.findAll()
      .thenCompose(connections -> accountRepository.findAll()
        .thenApply(accounts -> ApiResponse.success(Map.of("connections",
          connections.stream()
            .sorted(Comparator.comparing(BankConnection::createdAt))
            .map(connection -> information(connection, accounts))
            .toList()))));
  }

  /**
   * Syncs every account right away. The owner's address and browser go to the
   * bank as proof they are present, so this read does not count against the
   * four unattended reads PSD2 allows per day.
   */
  @RequestMapping(path = "/connections/sync/", method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> synchronize(
    HttpServletRequest request
  ) {
    var client = ClientAddress.of(request);
    return syncCoordinator.synchronizeNow(Map.of(
        "Psu-Ip-Address", client.value(),
        "Psu-User-Agent", client.userAgent()))
      .thenApply(started -> ApiResponse.success(Map.of("started", started)));
  }

  private Map<String, Object> information(
    BankConnection connection, List<BankAccount> accounts
  ) {
    var information = new HashMap<String, Object>();
    information.put("id", connection.id());
    information.put("space", connection.spaceId());
    information.put("bank_name", connection.bankName());
    information.put("bank_country", connection.bankCountry());
    information.put("valid_until", connection.validUntil());
    information.put("accounts", accounts.stream()
      .filter(account -> account.connectionId().equals(connection.id()))
      .map(this::information)
      .toList());
    return information;
  }

  private Map<String, Object> information(BankAccount account) {
    var information = new HashMap<String, Object>();
    information.put("id", account.id());
    information.put("name", account.name());
    information.put("iban", account.iban());
    information.put("currency", account.currency());
    information.put("balance", account.balance());
    information.put("last_synced_at", account.lastSyncedAt());
    information.put("sync_failure", account.syncFailure());
    information.put("spending", account.isSpendingAccount());
    return information;
  }
}