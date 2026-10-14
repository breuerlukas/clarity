package de.lukasbreuer.clarity.api.web.response;

import com.google.common.collect.Maps;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ApiResponse extends ResponseEntity<Map<String, Object>> {
  public static ApiResponse success() {
    return success(Maps.newHashMap());
  }

  public static ApiResponse success(Map<String, Object> data) {
    var body = Maps.<String, Object>newHashMap();
    body.put("success", true);
    body.putAll(data);
    return new ApiResponse(body, HttpStatus.OK);
  }

  public static ApiResponse error(int code) {
    return error(code, "");
  }

  public static ApiResponse error(int code, String message) {
    var body = Maps.<String, Object>newHashMap();
    body.put("success", false);
    var error = Maps.<String, Object>newHashMap();
    error.put("code", code);
    if (!message.isEmpty()) {
      error.put("message", message);
    }
    body.put("error", error);
    return new ApiResponse(body, HttpStatus.BAD_REQUEST);
  }

  private ApiResponse(Map<String, Object> body, HttpStatus status) {
    super(body, status);
  }

  public CompletableFuture<ApiResponse> future() {
    return CompletableFuture.completedFuture(this);
  }
}