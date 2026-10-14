package de.lukasbreuer.clarity.api.web.business;

import de.lukasbreuer.clarity.api.business.mail.MailCheck;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Fetches the new mails of a space's mailboxes now instead of waiting for
 * the next scheduled check. Error codes: 5001 unknown space.
 */
@RestController
@RequiredArgsConstructor
public final class MailboxCheckController {
  private final MailCheck mailCheck;

  /**
   * Answers whether a check started; false while one is still running.
   */
  @RequestMapping(path = "/business/mailboxes/check/",
    method = RequestMethod.POST)
  public ApiResponse check(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var spaceId = ApiRequestBody.of(payload, response).getUUID("space");
    if (spaceId == null) {
      return ApiResponse.error(5001);
    }
    return ApiResponse.success(Map.of("started", mailCheck.checkNow(spaceId)));
  }
}