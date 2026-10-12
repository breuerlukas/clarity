package de.lukasbreuer.clarity.api.web.authentication;

import de.lukasbreuer.clarity.api.owner.session.OwnerSession;
import de.lukasbreuer.clarity.api.owner.session.OwnerSessionRepository;
import de.lukasbreuer.clarity.api.owner.session.OwnerSessionStatus;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import de.lukasbreuer.clarity.api.web.security.ClientAddress;
import de.lukasbreuer.clarity.api.web.security.SessionCookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public final class SessionIssuer {
  private final SessionCookie sessionCookie;
  private final OwnerSessionRepository sessionRepository;

  /**
   * Opens a new session for the owner and hands its token to the browser as
   * the session cookie. The token itself is never stored.
   */
  public CompletableFuture<ApiResponse> issue(
    HttpServletRequest request, HttpServletResponse response
  ) {
    var token = sessionCookie.generateToken();
    var now = System.currentTimeMillis();
    var client = ClientAddress.of(request);
    var userAgent = client.userAgent();
    var session = OwnerSession.create(UUID.randomUUID(),
      sessionCookie.hash(token), OwnerSessionStatus.ACTIVE, client.value(),
      userAgent.substring(0, Math.min(userAgent.length(), 512)), now, now);
    return sessionRepository.save(session)
      .thenApply(_ -> signedIn(response, token));
  }

  private ApiResponse signedIn(HttpServletResponse response, String token) {
    sessionCookie.write(response, token);
    return ApiResponse.success();
  }
}