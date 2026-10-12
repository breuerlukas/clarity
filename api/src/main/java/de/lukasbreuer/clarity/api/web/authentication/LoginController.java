package de.lukasbreuer.clarity.api.web.authentication;

import de.lukasbreuer.clarity.api.hashing.Hashing;
import de.lukasbreuer.clarity.api.owner.LoginThrottle;
import de.lukasbreuer.clarity.api.owner.Owner;
import de.lukasbreuer.clarity.api.owner.OwnerRepository;
import de.lukasbreuer.clarity.api.owner.totp.TotpAuthenticator;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import de.lukasbreuer.clarity.api.web.security.ClientAddress;
import de.lukasbreuer.clarity.api.web.security.PublicEndpoint;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

/**
 * Name, password and a fresh authenticator code, all three or nothing. The
 * answer never says which part was wrong. Error codes: 1000 wrong
 * credentials, 1001 too many attempts.
 */
@RestController
@RequiredArgsConstructor
public final class LoginController {
  private final OwnerRepository ownerRepository;
  private final LoginThrottle throttle;
  private final Hashing hashing;
  private final TotpAuthenticator totpAuthenticator;
  private final SessionIssuer sessionIssuer;

  @PublicEndpoint
  @RequestMapping(path = "/login/", method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> login(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var address = ClientAddress.of(request).value();
    if (throttle.isBlocked(address)) {
      return ApiResponse.error(1001).future();
    }
    var credentials = Credentials.of(ApiRequestBody.of(payload, response));
    return ownerRepository.findFirstByConfirmed(true)
      .thenCompose(owner -> login(request, response, address, credentials,
        owner.orElse(null)));
  }

  private CompletableFuture<ApiResponse> login(
    HttpServletRequest request, HttpServletResponse response, String address,
    Credentials credentials, Owner owner
  ) {
    if (!isValid(owner, credentials)) {
      throttle.recordFailure(address);
      return ApiResponse.error(1000).future();
    }
    throttle.reset(address);
    owner.rememberTotpCode(credentials.code());
    return ownerRepository.save(owner)
      .thenCompose(_ -> sessionIssuer.issue(request, response));
  }

  private boolean isValid(Owner owner, Credentials credentials) {
    return owner != null && owner.name().equals(credentials.name()) &&
      hashing.matches(credentials.password(), owner.password()) &&
      !owner.isReplayedTotpCode(credentials.code()) &&
      totpAuthenticator.verify(owner.totpSecret(), credentials.code());
  }

  private record Credentials(String name, String password, String code) {
    static Credentials of(ApiRequestBody body) {
      return new Credentials(body.getString("name", 64).trim(),
        body.getString("password", 256), body.getString("code", 16));
    }
  }
}