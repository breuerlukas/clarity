package de.lukasbreuer.clarity.api.web.security;

import de.lukasbreuer.clarity.api.owner.session.OwnerSession;
import de.lukasbreuer.clarity.api.owner.session.OwnerSessionRepository;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

/**
 * Lets a request through only with a valid owner session, unless the handler
 * Spring actually resolved is a {@link PublicEndpoint}. Deciding on the
 * resolved handler instead of the raw URL means no path trick
 * (double slashes, encoded characters) can slip past it.
 */
@Component
@RequiredArgsConstructor
public final class OwnerInterceptor implements HandlerInterceptor {
  public static final String SESSION_ATTRIBUTE = "owner_session";

  private final SessionCookie sessionCookie;
  private final OwnerSessionRepository sessionRepository;

  @Override
  public boolean preHandle(
    HttpServletRequest request, HttpServletResponse response, Object handler
  ) {
    if (isAsyncResultDispatch(request) || isPublic(handler)) {
      return true;
    }
    var session = findValidSession(request);
    if (session.isEmpty()) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return false;
    }
    request.setAttribute(SESSION_ATTRIBUTE, session.get());
    touch(session.get());
    return true;
  }

  /**
   * Spring runs interceptors a second time when it hands back the result of a
   * {@code CompletableFuture}. The request was checked on the way in; checking
   * again would reject a logout whose session it just closed.
   */
  private boolean isAsyncResultDispatch(HttpServletRequest request) {
    return request.getDispatcherType() == DispatcherType.ASYNC;
  }

  private boolean isPublic(Object handler) {
    return handler instanceof HandlerMethod method &&
      method.hasMethodAnnotation(PublicEndpoint.class);
  }

  private Optional<OwnerSession> findValidSession(HttpServletRequest request) {
    return sessionCookie.read(request)
      .map(sessionCookie::hash)
      .flatMap(hash -> sessionRepository.findByTokenHash(hash).join())
      .filter(OwnerSession::isValid);
  }

  private void touch(OwnerSession session) {
    if (!session.needsTouch()) {
      return;
    }
    session.touch();
    sessionRepository.save(session);
  }
}