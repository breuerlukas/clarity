package de.lukasbreuer.clarity.api.web.settings;

import de.lukasbreuer.clarity.api.hashing.Hashing;
import de.lukasbreuer.clarity.api.owner.LoginThrottle;
import de.lukasbreuer.clarity.api.owner.Owner;
import de.lukasbreuer.clarity.api.owner.OwnerRepository;
import de.lukasbreuer.clarity.api.owner.session.OwnerSession;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import de.lukasbreuer.clarity.api.web.security.ClientAddress;
import de.lukasbreuer.clarity.api.web.security.OwnerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

/**
 * The owner's name and password. A new password needs the current one and
 * signs out every other session. Error codes: 1000 wrong current password,
 * 1001 too many attempts, 1003 new password shorter than 12 characters,
 * 1006 empty name.
 */
@RestController
@RequiredArgsConstructor
public final class AccountController {
  private static final int MINIMUM_PASSWORD_LENGTH = 12;

  private final OwnerRepository ownerRepository;
  private final Hashing hashing;
  private final LoginThrottle throttle;
  private final SessionClosing sessionClosing;

  @RequestMapping(path = "/account/name/", method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> rename(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var name = ApiRequestBody.of(payload, response).getString("name", 64).trim();
    if (name.isEmpty()) {
      return ApiResponse.error(1006).future();
    }
    return ownerRepository.findFirstByConfirmed(true)
      .thenApply(owner -> owner.orElseThrow())
      .thenCompose(owner -> rename(owner, name));
  }

  private CompletableFuture<ApiResponse> rename(Owner owner, String name) {
    owner.rename(name);
    return ownerRepository.save(owner).thenApply(_ -> ApiResponse.success());
  }

  @RequestMapping(path = "/account/password/", method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> changePassword(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var address = ClientAddress.of(request).value();
    if (throttle.isBlocked(address)) {
      return ApiResponse.error(1001).future();
    }
    var body = ApiRequestBody.of(payload, response);
    var change = PasswordChange.of(body.getString("current_password", 256),
      body.getString("new_password", 256));
    if (change.next().length() < MINIMUM_PASSWORD_LENGTH) {
      return ApiResponse.error(1003).future();
    }
    var session = (OwnerSession) request.getAttribute(
      OwnerInterceptor.SESSION_ATTRIBUTE);
    return ownerRepository.findFirstByConfirmed(true)
      .thenApply(owner -> owner.orElseThrow())
      .thenCompose(owner -> changePassword(owner, change, address, session));
  }

  private CompletableFuture<ApiResponse> changePassword(
    Owner owner, PasswordChange change, String address, OwnerSession session
  ) {
    if (!hashing.matches(change.current(), owner.password())) {
      throttle.recordFailure(address);
      return ApiResponse.error(1000).future();
    }
    owner.changePassword(hashing.hash(change.next()));
    return ownerRepository.save(owner)
      .thenCompose(_ -> sessionClosing.closeAllExcept(session.id()))
      .thenApply(_ -> ApiResponse.success());
  }

  private record PasswordChange(String current, String next) {
    static PasswordChange of(String current, String next) {
      return new PasswordChange(current, next);
    }
  }
}
