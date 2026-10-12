package de.lukasbreuer.clarity.api.web.security;

import com.google.common.hash.Hashing;
import de.lukasbreuer.clarity.api.web.WebConfiguration;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

/**
 * The session token lives in an HttpOnly cookie, so no script on the page,
 * injected or not, can ever read it. SameSite=Strict keeps other sites from
 * riding along on it.
 */
@Component
@RequiredArgsConstructor
public final class SessionCookie {
  private static final String NAME = "clarity_session";
  private static final Duration LIFETIME = Duration.ofDays(30);

  private final WebConfiguration webConfiguration;
  private final SecureRandom random = new SecureRandom();

  public String generateToken() {
    var bytes = new byte[32];
    random.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  public String hash(String token) {
    return Hashing.sha256().hashString(token, StandardCharsets.UTF_8)
      .toString();
  }

  public Optional<String> read(HttpServletRequest request) {
    return Optional.ofNullable(request.getCookies())
      .flatMap(cookies -> Arrays.stream(cookies)
        .filter(cookie -> NAME.equals(cookie.getName()))
        .map(Cookie::getValue)
        .findFirst());
  }

  public void write(HttpServletResponse response, String token) {
    response.addHeader(HttpHeaders.SET_COOKIE, build(token, LIFETIME));
  }

  public void clear(HttpServletResponse response) {
    response.addHeader(HttpHeaders.SET_COOKIE, build("", Duration.ZERO));
  }

  private String build(String value, Duration maxAge) {
    return ResponseCookie.from(NAME, value)
      .httpOnly(true)
      .secure(webConfiguration.secureCookies())
      .sameSite("Strict")
      .path("/")
      .maxAge(maxAge)
      .build()
      .toString();
  }
}