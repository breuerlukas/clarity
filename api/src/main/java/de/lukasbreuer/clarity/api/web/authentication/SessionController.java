package de.lukasbreuer.clarity.api.web.authentication;

import de.lukasbreuer.clarity.api.owner.OwnerRepository;
import de.lukasbreuer.clarity.api.owner.session.OwnerSession;
import de.lukasbreuer.clarity.api.owner.session.OwnerSessionRepository;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import de.lukasbreuer.clarity.api.web.security.OwnerInterceptor;
import de.lukasbreuer.clarity.api.web.security.SessionCookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequiredArgsConstructor
public final class SessionController {
  private final OwnerRepository ownerRepository;
  private final OwnerSessionRepository sessionRepository;
  private final SessionCookie sessionCookie;

  /**
   * Answers 401 through the interceptor when there is no valid session, so the
   * panel only has to look at the status code.
   */
  @RequestMapping(path = "/session/", method = RequestMethod.GET)
  public CompletableFuture<ApiResponse> session() {
    return ownerRepository.findFirstByConfirmed(true)
      .thenApply(owner -> ApiResponse.success(Map.of("name",
        owner.map(found -> found.name()).orElse(""))));
  }

  @RequestMapping(path = "/logout/", method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> logout(
    HttpServletRequest request, HttpServletResponse response
  ) {
    var session = (OwnerSession) request.getAttribute(
      OwnerInterceptor.SESSION_ATTRIBUTE);
    session.close();
    sessionCookie.clear(response);
    return sessionRepository.save(session)
      .thenApply(_ -> ApiResponse.success());
  }
}