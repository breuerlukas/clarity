package de.lukasbreuer.clarity.api.web.cash;

import de.lukasbreuer.clarity.api.space.SpaceRepository;
import de.lukasbreuer.clarity.api.transaction.TransactionRepository;
import de.lukasbreuer.clarity.api.transaction.cash.CashBalances;
import de.lukasbreuer.clarity.api.transaction.cash.CashWallet;
import de.lukasbreuer.clarity.api.transaction.cash.CashWallets;
import de.lukasbreuer.clarity.api.transaction.category.Category;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;
import de.lukasbreuer.clarity.api.web.request.UuidParameter;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * The cash wallet of a space and counting it. A count books the difference
 * to the expected balance as a correction entry without a counterparty in
 * the transfer category: it shows in the wallet's history, but counts as
 * neither spending nor income. Error codes: 5001 unknown space, 8002
 * invalid counted amount.
 */
@RestController
@RequiredArgsConstructor
public final class CashController {
  private static final BigDecimal MAXIMUM_BALANCE =
    new BigDecimal("1000000000");

  private final SpaceRepository spaceRepository;
  private final TransactionRepository transactionRepository;
  private final CashWallets wallets;
  private final CashBalances balances;

  @RequestMapping(path = "/cash/", method = RequestMethod.GET)
  public CompletableFuture<ApiResponse> findWallet(
    @RequestParam("space") String space
  ) {
    var spaceId = UuidParameter.parse(space);
    if (spaceId.isEmpty()) {
      return ApiResponse.error(5001).future();
    }
    return spaceRepository.existsById(spaceId.get())
      .thenCompose(exists -> exists ? wallets.of(spaceId.get())
        .thenCompose(this::walletResponse) : ApiResponse.error(5001).future());
  }

  @RequestMapping(path = "/cash/count/", method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> count(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var spaceId = body.getUUID("space");
    var counted = countedBalance(body);
    if (spaceId == null) {
      return ApiResponse.error(5001).future();
    }
    if (counted.isEmpty()) {
      return ApiResponse.error(8002).future();
    }
    return spaceRepository.existsById(spaceId)
      .thenCompose(exists -> exists ? wallets.of(spaceId)
        .thenCompose(wallet -> correct(wallet, counted.get())) :
        ApiResponse.error(5001).future());
  }

  private Optional<BigDecimal> countedBalance(ApiRequestBody body) {
    try {
      return Optional.of(new BigDecimal(body.getString("balance", 24)))
        .filter(balance -> balance.signum() >= 0)
        .filter(balance -> balance.stripTrailingZeros().scale() <= 2)
        .filter(balance -> balance.compareTo(MAXIMUM_BALANCE) < 0);
    } catch (NumberFormatException exception) {
      return Optional.empty();
    }
  }

  private CompletableFuture<ApiResponse> correct(
    CashWallet wallet, BigDecimal counted
  ) {
    return balances.of(wallet)
      .thenCompose(balance -> counted.compareTo(balance) == 0 ?
        CompletableFuture.completedFuture(null) :
        transactionRepository.save(wallet.entry(LocalDate.now(),
          counted.subtract(balance), "", "", Category.TRANSFER)))
      .thenCompose(_ -> walletResponse(wallet));
  }

  private CompletableFuture<ApiResponse> walletResponse(CashWallet wallet) {
    return balances.of(wallet)
      .thenApply(balance -> ApiResponse.success(Map.of("wallet",
        wallet.information(balance))));
  }
}