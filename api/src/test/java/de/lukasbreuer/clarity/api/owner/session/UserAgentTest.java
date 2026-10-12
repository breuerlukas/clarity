package de.lukasbreuer.clarity.api.owner.session;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class UserAgentTest {
  @Test
  void recognizesEdgeOnWindowsDespiteTheChromeAndSafariTokens() {
    var userAgent = UserAgent.of("Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
      "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0 Safari/537.36 " +
      "Edg/140.0");
    assertEquals("Windows", userAgent.platform());
    assertEquals("Edge", userAgent.browser());
  }

  @Test
  void recognizesAndroidBeforeLinux() {
    var userAgent = UserAgent.of("Mozilla/5.0 (Linux; Android 16) " +
      "AppleWebKit/537.36 Chrome/140.0 Mobile Safari/537.36");
    assertEquals("Android", userAgent.platform());
    assertEquals("Chrome", userAgent.browser());
    assertTrue(userAgent.isMobile());
  }

  @Test
  void staysEmptyWithoutUserAgent() {
    assertEquals("", UserAgent.of(null).platform());
  }
}
