package de.lukasbreuer.clarity.api.web.authentication;

import de.lukasbreuer.clarity.api.owner.LoginThrottle;
import de.lukasbreuer.clarity.api.owner.Owner;
import de.lukasbreuer.clarity.api.owner.OwnerRepository;
import de.lukasbreuer.clarity.api.owner.totp.TotpAuthenticator;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * The second setup step: the owner stays pending until the first code from
 * their authenticator app checks out. Error 1004 means the code was wrong.
 */
@Component
@RequiredArgsConstructor
public final class OwnerConfirmation {
  private final OwnerRepository ownerRepository;
  private final TotpAuthenticator totpAuthenticator;
  private final LoginThrottle throttle;
  private final SessionIssuer sessionIssuer;

  public CompletableFuture<Void> removePendingOwner() {
    return ownerRepository.findFirstByConfirmed(false)
      .thenCompose(owner -> owner.map(ownerRepository::delete)
        .orElse(CompletableFuture.completedFuture(null)));
  }

  public CompletableFuture<ApiResponse> confirm(
    HttpServletRequest request, HttpServletResponse response, String address,
    String code
  ) {
    return ownerRepository.existsByConfirmed(true)
      .thenCompose(exists -> exists ?
        CompletableFuture.completedFuture(Optional.<Owner>empty()) :
        ownerRepository.findFirstByConfirmed(false))
      .thenCompose(owner -> confirm(request, response, address, code,
        owner.orElse(null)));
  }

  private CompletableFuture<ApiResponse> confirm(
    HttpServletRequest request, HttpServletResponse response, String address,
    String code, Owner owner
  ) {
    if (owner == null) {
      return ApiResponse.error(1002).future();
    }
    if (!totpAuthenticator.verify(owner.totpSecret(), code)) {
      throttle.recordFailure(address);
      return ApiResponse.error(1004).future();
    }
    owner.confirm();
    owner.rememberTotpCode(code);
    return ownerRepository.save(owner)
      .thenCompose(_ -> sessionIssuer.issue(request, response));
  }
}