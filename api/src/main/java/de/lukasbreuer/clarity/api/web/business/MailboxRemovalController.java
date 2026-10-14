package de.lukasbreuer.clarity.api.web.business;

import de.lukasbreuer.clarity.api.business.mail.MailFolderCursorRepository;
import de.lukasbreuer.clarity.api.business.mail.Mailbox;
import de.lukasbreuer.clarity.api.business.mail.MailboxRepository;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Disconnects a mailbox from its space. Error codes: 6001 unknown mailbox.
 */
@RestController
@RequiredArgsConstructor
public final class MailboxRemovalController {
  private final MailboxRepository mailboxRepository;
  private final MailFolderCursorRepository cursorRepository;

  /**
   * Imported invoices stay; only the connection goes.
   */
  @RequestMapping(path = "/business/mailboxes/delete/",
    method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> delete(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var mailboxId = ApiRequestBody.of(payload, response).getUUID("id");
    if (mailboxId == null) {
      return ApiResponse.error(6001).future();
    }
    return mailboxRepository.findById(mailboxId).thenCompose(this::remove);
  }

  private CompletableFuture<ApiResponse> remove(Optional<Mailbox> mailbox) {
    if (mailbox.isEmpty()) {
      return ApiResponse.error(6001).future();
    }
    cursorRepository.deleteByMailboxId(mailbox.get().id());
    return mailboxRepository.delete(mailbox.get())
      .thenApply(_ -> ApiResponse.success());
  }
}