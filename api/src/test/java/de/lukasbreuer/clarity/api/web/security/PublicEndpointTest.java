package de.lukasbreuer.clarity.api.web.security;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every endpoint needs the owner unless it is marked public. This pins the
 * public ones down, so a new endpoint can only become reachable without a
 * session on purpose, with this list changed in the same pull request.
 */
final class PublicEndpointTest {
  private static final Set<String> PUBLIC_ENDPOINTS = Set.of(
    "GET /setup/status/",
    "POST /setup/",
    "POST /setup/confirm/",
    "POST /login/");

  @Test
  void onlySetupAndLoginArePublic() {
    var publicEndpoints = endpoints().stream()
      .filter(method -> method.isAnnotationPresent(PublicEndpoint.class))
      .flatMap(method -> routes(method).stream())
      .collect(Collectors.toSet());
    assertEquals(PUBLIC_ENDPOINTS, publicEndpoints);
  }

  /**
   * A mapping without a method would answer every verb, including ones the
   * request guard lets through without the panel's header.
   */
  @Test
  void everyEndpointNamesItsMethodAndPath() {
    var endpoints = endpoints();
    assertFalse(endpoints.isEmpty());
    for (var endpoint : endpoints) {
      var mapping = endpoint.getAnnotation(RequestMapping.class);
      assertTrue(mapping.method().length > 0, endpoint.toString());
      assertTrue(mapping.path().length > 0, endpoint.toString());
    }
  }

  private List<Method> endpoints() {
    var scanner = new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
    return scanner.findCandidateComponents("de.lukasbreuer.clarity.api")
      .stream()
      .map(definition -> load(definition.getBeanClassName()))
      .flatMap(type -> Arrays.stream(type.getDeclaredMethods()))
      .filter(method -> method.isAnnotationPresent(RequestMapping.class))
      .toList();
  }

  private Set<String> routes(Method method) {
    var mapping = method.getAnnotation(RequestMapping.class);
    return Arrays.stream(mapping.method())
      .flatMap(verb -> Arrays.stream(mapping.path())
        .map(path -> verb.name() + " " + path))
      .collect(Collectors.toSet());
  }

  private Class<?> load(String name) {
    try {
      return Class.forName(name);
    } catch (ClassNotFoundException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
