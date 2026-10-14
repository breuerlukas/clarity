package de.lukasbreuer.clarity.api.web.settings;

import de.lukasbreuer.clarity.api.owner.session.OwnerSession;
import de.lukasbreuer.clarity.api.owner.session.UserAgent;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import de.lukasbreuer.clarity.api.web.security.OwnerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Where the owner is signed in, and a way out for each of those places.
 * Error code 3100: unknown session.
 */
@RestController
@RequiredArgsConstructor
public final class SessionSettingsController {
  private final SessionClosing sessionClosing;

  @RequestMapping(path = "/sessions/", method = RequestMethod.GET)
  public CompletableFuture<ApiResponse> findSessions(
    HttpServletRequest request
  ) {
    var current = currentSession(request).id();
    return sessionClosing.activeSessions()
      .thenApply(sessions -> ApiResponse.success(Map.of("sessions",
        sessions.stream()
          .sorted(Comparator.comparing(OwnerSession::lastSeenAt).reversed())
          .map(session -> information(session, current))
          .toList())));
  }

  @RequestMapping(path = "/sessions/close/", method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> closeSession(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var sessionId = ApiRequestBody.of(payload, response).getUUID("id");
    if (sessionId == null) {
      return ApiResponse.error(3100).future();
    }
    return sessionClosing.activeSessions()
      .thenApply(sessions -> sessions.stream()
        .filter(session -> session.id().equals(sessionId)).toList())
      .thenCompose(sessionClosing::close)
      .thenApply(closed -> closed == 0 ? ApiResponse.error(3100) :
        ApiResponse.success());
  }

  @RequestMapping(path = "/sessions/close-others/", method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> closeOtherSessions(
    HttpServletRequest request
  ) {
    return sessionClosing.closeAllExcept(currentSession(request).id())
      .thenApply(closed -> ApiResponse.success(Map.of("closed", closed)));
  }

  private OwnerSession currentSession(HttpServletRequest request) {
    return (OwnerSession) request.getAttribute(
      OwnerInterceptor.SESSION_ATTRIBUTE);
  }

  private Map<String, Object> information(OwnerSession session, UUID current) {
    var userAgent = UserAgent.of(session.userAgent());
    return Map.of("id", session.id(),
      "ip_address", session.ipAddress() == null ? "" : session.ipAddress(),
      "platform", userAgent.platform(),
      "browser", userAgent.browser(),
      "mobile", userAgent.isMobile(),
      "opened_at", session.openedAt(),
      "last_seen_at", session.lastSeenAt(),
      "current", session.id().equals(current));
  }
}
