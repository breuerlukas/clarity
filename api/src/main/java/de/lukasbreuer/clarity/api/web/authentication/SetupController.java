package de.lukasbreuer.clarity.api.web.authentication;

import de.lukasbreuer.clarity.api.hashing.Hashing;
import de.lukasbreuer.clarity.api.owner.LoginThrottle;
import de.lukasbreuer.clarity.api.owner.Owner;
import de.lukasbreuer.clarity.api.owner.OwnerRepository;
import de.lukasbreuer.clarity.api.owner.setup.SetupCode;
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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Creates the owner in two steps: name, password and the setup code from the
 * log first, then a code from the authenticator app to prove it was set up.
 * Error codes: 1001 too many attempts, 1002 wrong setup code or an owner
 * already exists, 1003 password shorter than 12 characters.
 */
@RestController
@RequiredArgsConstructor
public final class SetupController {
  private static final int MINIMUM_PASSWORD_LENGTH = 12;

  private final OwnerRepository ownerRepository;
  private final SetupCode setupCode;
  private final LoginThrottle throttle;
  private final Hashing hashing;
  private final TotpAuthenticator totpAuthenticator;
  private final OwnerConfirmation ownerConfirmation;

  @PublicEndpoint
  @RequestMapping(path = "/setup/status/", method = RequestMethod.GET)
  public CompletableFuture<ApiResponse> status() {
    return ownerRepository.existsByConfirmed(true)
      .thenApply(exists -> ApiResponse.success(Map.of("setup_required",
        !exists)));
  }

  @PublicEndpoint
  @RequestMapping(path = "/setup/", method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> setup(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var address = ClientAddress.of(request).value();
    var rejection = rejectSetup(address, body.getString("setup_code"));
    if (rejection != null) {
      return rejection.future();
    }
    var password = body.getString("password", 256);
    if (password.length() < MINIMUM_PASSWORD_LENGTH) {
      return ApiResponse.error(1003).future();
    }
    var name = body.getString("name", 64).trim();
    return ownerRepository.existsByConfirmed(true)
      .thenCompose(exists -> exists ? ApiResponse.error(1002).future() :
        createOwner(name, password));
  }

  @PublicEndpoint
  @RequestMapping(path = "/setup/confirm/", method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> confirm(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var address = ClientAddress.of(request).value();
    var rejection = rejectSetup(address, body.getString("setup_code"));
    if (rejection != null) {
      return rejection.future();
    }
    return ownerConfirmation.confirm(request, response, address,
      body.getString("code", 16));
  }

  private ApiResponse rejectSetup(String address, String code) {
    if (throttle.isBlocked(address)) {
      return ApiResponse.error(1001);
    }
    if (!setupCode.matches(code)) {
      throttle.recordFailure(address);
      return ApiResponse.error(1002);
    }
    return null;
  }

  private CompletableFuture<ApiResponse> createOwner(
    String name, String password
  ) {
    var owner = Owner.create(UUID.randomUUID(), name, hashing.hash(password),
      totpAuthenticator.generateSecret(), false, null, 0,
      System.currentTimeMillis());
    return ownerConfirmation.removePendingOwner()
      .thenCompose(_ -> ownerRepository.save(owner))
      .thenApply(this::setupResponse);
  }

  private ApiResponse setupResponse(Owner owner) {
    try {
      return ApiResponse.success(Map.of(
        "totp_uri", totpAuthenticator.uri(owner.name(), owner.totpSecret()),
        "totp_secret", owner.totpSecret(),
        "qr_code", totpAuthenticator.qrCode(owner.name(), owner.totpSecret())));
    } catch (Exception exception) {
      return ApiResponse.error(1005);
    }
  }
}