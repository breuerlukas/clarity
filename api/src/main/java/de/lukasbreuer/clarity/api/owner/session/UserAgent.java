package de.lukasbreuer.clarity.api.owner.session;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Turns a user agent string into something a person recognizes in the
 * session list, like "Firefox on Linux". Order matters: Edge and Chrome both
 * claim to be Safari, and Android claims to be Linux.
 */
@RequiredArgsConstructor(staticName = "of")
public final class UserAgent {
  private static final List<Map.Entry<String, String>> PLATFORMS = List.of(
    Map.entry("Android", "Android"), Map.entry("iPhone", "iOS"),
    Map.entry("iPad", "iPadOS"), Map.entry("Windows", "Windows"),
    Map.entry("Mac OS", "macOS"), Map.entry("CrOS", "ChromeOS"),
    Map.entry("Linux", "Linux"));
  private static final List<Map.Entry<String, String>> BROWSERS = List.of(
    Map.entry("Edg/", "Edge"), Map.entry("OPR/", "Opera"),
    Map.entry("Firefox/", "Firefox"), Map.entry("Chrome/", "Chrome"),
    Map.entry("Safari/", "Safari"));

  private final String value;

  public String platform() {
    return find(PLATFORMS);
  }

  public String browser() {
    return find(BROWSERS);
  }

  public boolean isMobile() {
    return value != null && (value.contains("Mobile") ||
      value.contains("Android"));
  }

  private String find(List<Map.Entry<String, String>> candidates) {
    return candidates.stream()
      .filter(candidate -> value != null && value.contains(candidate.getKey()))
      .map(Map.Entry::getValue)
      .findFirst()
      .orElse("");
  }
}
