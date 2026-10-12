package de.lukasbreuer.clarity.api.web.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Second line against cross-site requests next to the SameSite cookie: every
 * state-changing request has to carry a custom header. Browsers refuse to send
 * custom headers cross-origin without a CORS preflight, and this server
 * answers no preflight, so only the panel itself can produce such a request.
 */
@Component
public final class RequestGuardFilter extends OncePerRequestFilter {
  public static final String REQUEST_HEADER = "X-Clarity-Request";
  private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD");

  @Override
  protected void doFilterInternal(
    HttpServletRequest request, HttpServletResponse response,
    FilterChain filterChain
  ) throws ServletException, IOException {
    response.setHeader("Cache-Control", "no-store");
    response.setHeader("X-Content-Type-Options", "nosniff");
    if (!isTrusted(request)) {
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      return;
    }
    filterChain.doFilter(request, response);
  }

  private boolean isTrusted(HttpServletRequest request) {
    return SAFE_METHODS.contains(request.getMethod()) ||
      "1".equals(request.getHeader(REQUEST_HEADER));
  }
}