package de.lukasbreuer.clarity.api.web.transaction;

import de.lukasbreuer.clarity.api.space.SpaceRepository;
import de.lukasbreuer.clarity.api.transaction.recurring.RecurringSettingRepository;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Stores what the owner decided about a fixed cost: cancelled with an end
 * date, "not ended" for a payment wrongly taken as stopped, the notice
 * period to be reminded of, and "not a fixed cost" for bookings wrongly
 * taken as one. Error codes: 3008 invalid values, 5001 unknown
 * space.
 */
@RestController
@RequiredArgsConstructor
public final class RecurringSettingController {
  private static final int MAXIMUM_KEY_LENGTH = 500;

  private final SpaceRepository spaceRepository;
  private final RecurringSettingRepository settingRepository;

  @RequestMapping(path = "/recurring/settings/", method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> update(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var spaceId = body.getUUID("space");
    var key = body.getString("key", MAXIMUM_KEY_LENGTH);
    var draft = RecurringSettingDraft.parse(body);
    if (spaceId == null) {
      return ApiResponse.error(5001).future();
    }
    if (key.isBlank() || draft.isEmpty()) {
      return ApiResponse.error(3008).future();
    }
    return spaceRepository.existsById(spaceId)
      .thenCompose(exists -> exists ? save(spaceId, key, draft.get()) :
        ApiResponse.error(5001).future());
  }

  private CompletableFuture<ApiResponse> save(
    UUID spaceId, String key, RecurringSettingDraft draft
  ) {
    return settingRepository.findBySpaceIdAndPaymentKey(spaceId, key)
      .thenApply(setting -> setting.map(draft::applyTo)
        .orElseGet(() -> draft.create(spaceId, key)))
      .thenCompose(settingRepository::save)
      .thenApply(_ -> ApiResponse.success());
  }
}
