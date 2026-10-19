package de.lukasbreuer.clarity.api.web.request;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.IntStream;

@RequiredArgsConstructor(staticName = "create")
public final class ApiRequestBody {
  public static ApiRequestBody of(
    String payload, HttpServletResponse response
  ) {
    return create(new JSONObject(payload), response);
  }

  private final JSONObject body;
  private final HttpServletResponse response;

  /**
   * Is used to get a string from request body
   * @param key The key to find the content
   * @return The value behind the key
   */
  public String getString(String key) {
    if (!body.has(key)) {
      failure();
      return "";
    }
    return body.optString(key, "");
  }

  /**
   * Is used to get a string from request body
   * @param key The key to find the content
   * @param maxLength The maximum length of the content (if the content is
   *                  longer, it is cut, so it matches the maximum length)
   * @return The value behind the key
   */
  public String getString(String key, int maxLength) {
    var value = getString(key);
    return value.substring(0, Math.min(maxLength, value.length()));
  }

  /**
   * Is used to get a long from request body
   * @param key The key to find the content
   * @return The value behind the key
   */
  public long getLong(String key) {
    if (!body.has(key)) {
      failure();
      return -1;
    }
    return body.optLong(key, -1);
  }

  /**
   * Is used to get a boolean from request body
   * @param key The key to find the content
   * @return The value behind the key
   */
  public boolean getBoolean(String key) {
    if (!body.has(key)) {
      failure();
      return false;
    }
    return body.optBoolean(key, false);
  }

  /**
   * Is used to get a list of strings from request body
   * @param key The key to find the content
   * @return The values behind the key, or an empty list if it is no array
   */
  public List<String> getStringList(String key) {
    var array = body.optJSONArray(key);
    if (array == null) {
      failure();
      return List.of();
    }
    return IntStream.range(0, array.length()).mapToObj(array::optString)
      .toList();
  }

  /**
   * Is used to get a list of objects from request body
   * @param key The key to find the content
   * @return The objects behind the key, each read like a request body, or an
   *         empty list if it is no array
   */
  public List<ApiRequestBody> getBodyList(String key) {
    var array = body.optJSONArray(key);
    if (array == null) {
      failure();
      return List.of();
    }
    return IntStream.range(0, array.length()).mapToObj(array::optJSONObject)
      .filter(Objects::nonNull)
      .map(object -> create(object, response))
      .toList();
  }

  /**
   * Is used to get an uuid from request body
   * @param key The key to find the content
   * @return The value behind the key, or null if it is no valid uuid
   */
  public UUID getUUID(String key) {
    try {
      return UUID.fromString(getString(key));
    } catch (Exception exception) {
      failure();
      return null;
    }
  }

  private void failure() {
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    response.setContentLength(0);
  }
}