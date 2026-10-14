package de.lukasbreuer.clarity.api.web.security;

import de.lukasbreuer.clarity.api.web.WebConfiguration;
import org.apache.commons.configuration2.INIConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SessionCookieTest {
  @Test
  void keepsTheTokenAwayFromScriptsAndOtherSites() {
    var cookie = written(true);
    assertTrue(cookie.contains("HttpOnly"), cookie);
    assertTrue(cookie.contains("SameSite=Strict"), cookie);
    assertTrue(cookie.contains("Secure"), cookie);
    assertTrue(cookie.contains("Path=/"), cookie);
  }

  @Test
  void dropsSecureOnlyWhenConfiguredForPlainHttp() {
    assertFalse(written(false).contains("Secure"));
  }

  @Test
  void issuesLongRandomTokensAndStoresOnlyTheirHash() {
    var cookie = sessionCookie(true);
    var token = cookie.generateToken();
    assertEquals(43, token.length());
    assertNotEquals(token, cookie.generateToken());
    assertEquals(64, cookie.hash(token).length());
    assertNotEquals(token, cookie.hash(token));
  }

  private String written(boolean secure) {
    var response = new MockHttpServletResponse();
    sessionCookie(secure).write(response, "token");
    return response.getHeader("Set-Cookie");
  }

  private SessionCookie sessionCookie(boolean secure) {
    var file = new INIConfiguration();
    file.setProperty("web.port", 8080);
    file.setProperty("web.secure_cookies", secure);
    var configuration = WebConfiguration.create();
    configuration.load(file);
    return new SessionCookie(configuration);
  }
}
