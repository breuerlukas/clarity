package de.lukasbreuer.clarity.api.web.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

final class RequestGuardFilterTest {
  private final RequestGuardFilter filter = new RequestGuardFilter();

  @Test
  void refusesStateChangingRequestsWithoutThePanelHeader() throws Exception {
    for (var method : new String[]{"POST", "PUT", "PATCH", "DELETE"}) {
      var chain = new MockFilterChain();
      var response = run(new MockHttpServletRequest(method, "/v1/logout/"),
        chain);
      assertEquals(403, response.getStatus(), method);
      assertNull(chain.getRequest(), method);
    }
  }

  @Test
  void refusesAWrongHeaderValue() throws Exception {
    var request = new MockHttpServletRequest("POST", "/v1/logout/");
    request.addHeader(RequestGuardFilter.REQUEST_HEADER, "true");
    assertEquals(403, run(request, new MockFilterChain()).getStatus());
  }

  @Test
  void letsThePanelAndReadsThrough() throws Exception {
    var post = new MockHttpServletRequest("POST", "/v1/logout/");
    post.addHeader(RequestGuardFilter.REQUEST_HEADER, "1");
    var chain = new MockFilterChain();
    run(post, chain);
    assertNotNull(chain.getRequest());
    var readChain = new MockFilterChain();
    run(new MockHttpServletRequest("GET", "/v1/session/"), readChain);
    assertNotNull(readChain.getRequest());
  }

  @Test
  void forbidsCachingAndSniffingOfEveryAnswer() throws Exception {
    var response = run(new MockHttpServletRequest("POST", "/v1/login/"),
      new MockFilterChain());
    assertEquals("no-store", response.getHeader("Cache-Control"));
    assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
  }

  private MockHttpServletResponse run(
    MockHttpServletRequest request, MockFilterChain chain
  ) throws Exception {
    var response = new MockHttpServletResponse();
    filter.doFilter(request, response, chain);
    return response;
  }
}
