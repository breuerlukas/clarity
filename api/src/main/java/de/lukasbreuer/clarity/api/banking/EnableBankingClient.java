package de.lukasbreuer.clarity.api.banking;

import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Thin client for the Enable Banking REST API. Callers pass the path and get
 * the parsed JSON back; anything but a 2xx fails the future with an
 * {@link EnableBankingException}.
 */
@Component
@RequiredArgsConstructor
public final class EnableBankingClient {
  private static final String BASE_URL = "https://api.enablebanking.com";

  private final HttpClient bankingHttpClient;
  private final EnableBankingSigner signer;

  public CompletableFuture<JSONObject> get(String path) {
    return get(path, Map.of());
  }

  /**
   * @param headers Extra headers, such as Psu-Ip-Address when the owner
   *                triggered the request and is present
   */
  public CompletableFuture<JSONObject> get(
    String path, Map<String, String> headers
  ) {
    return send(() -> prepare(path, headers).GET());
  }

  public CompletableFuture<JSONObject> post(String path, JSONObject body) {
    return send(() -> prepare(path, Map.of())
      .header("Content-Type", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString(body.toString())));
  }

  public CompletableFuture<JSONObject> delete(String path) {
    return send(() -> prepare(path, Map.of()).DELETE());
  }

  private HttpRequest.Builder prepare(
    String path, Map<String, String> headers
  ) {
    var builder = HttpRequest.newBuilder(URI.create(BASE_URL + path))
      .timeout(Duration.ofSeconds(60))
      .header("Authorization", "Bearer " + token());
    headers.forEach(builder::header);
    return builder;
  }

  private String token() {
    try {
      return signer.token();
    } catch (Exception exception) {
      throw new IllegalStateException("Enable Banking is not set up",
        exception);
    }
  }

  /**
   * Builds the request inside the future, so a missing key fails the future
   * instead of throwing at the caller.
   */
  private CompletableFuture<JSONObject> send(
    Supplier<HttpRequest.Builder> request
  ) {
    return CompletableFuture.completedFuture(request)
      .thenApply(supplier -> supplier.get().build())
      .thenCompose(built -> bankingHttpClient.sendAsync(built,
        HttpResponse.BodyHandlers.ofString()))
      .thenApply(this::parse);
  }

  private JSONObject parse(HttpResponse<String> response) {
    if (response.statusCode() / 100 != 2) {
      throw new EnableBankingException(response.statusCode());
    }
    var body = response.body();
    return body == null || body.isBlank() ? new JSONObject() :
      new JSONObject(body);
  }
}