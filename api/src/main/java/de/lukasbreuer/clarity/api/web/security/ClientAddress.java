package de.lukasbreuer.clarity.api.web.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * The address of the browser behind the panel's nginx. Trusting X-Real-IP is
 * safe here only because the api is never published itself: nginx is the one
 * thing that can reach it, and nginx overwrites the header on every request.
 */
@RequiredArgsConstructor(staticName = "of")
public final class ClientAddress {
  private final HttpServletRequest request;

  public String value() {
    var forwarded = request.getHeader("X-Real-IP");
    if (forwarded == null || forwarded.isBlank()) {
      return request.getRemoteAddr();
    }
    return forwarded;
  }

  public String userAgent() {
    var userAgent = request.getHeader("User-Agent");
    return userAgent == null ? "" : userAgent;
  }
}