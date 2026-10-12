package de.lukasbreuer.clarity.api.web.banking;

import de.lukasbreuer.clarity.api.banking.account.BankAccount;
import de.lukasbreuer.clarity.api.banking.account.BankAccountRepository;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Decides which accounts count towards the money left until payday. Error
 * code 2004: unknown account.
 */
@RestController
@RequiredArgsConstructor
public final class AccountController {
  private final BankAccountRepository accountRepository;

  @RequestMapping(path = "/accounts/spending/", method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> markSpending(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var accountId = body.getUUID("id");
    if (accountId == null) {
      return ApiResponse.error(2004).future();
    }
    var spending = body.getBoolean("spending");
    return accountRepository.findById(accountId)
      .thenCompose(account -> markSpending(account, spending));
  }

  private CompletableFuture<ApiResponse> markSpending(
    Optional<BankAccount> account, boolean spending
  ) {
    if (account.isEmpty()) {
      return ApiResponse.error(2004).future();
    }
    account.get().markSpending(spending);
    return accountRepository.save(account.get())
      .thenApply(_ -> ApiResponse.success());
  }
}
