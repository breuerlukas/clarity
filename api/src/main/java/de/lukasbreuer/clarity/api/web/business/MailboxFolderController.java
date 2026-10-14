package de.lukasbreuer.clarity.api.web.business;

import de.lukasbreuer.clarity.api.business.mail.MailboxRepository;
import de.lukasbreuer.clarity.api.secret.SecretBox;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;
import de.lukasbreuer.clarity.api.web.request.UuidParameter;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Logs in with the details the owner entered and lists the folders, so the
 * panel can offer them before a mailbox is saved. Error codes: 6000 invalid
 * mailbox, 6002 connection or login failed.
 */
@RestController
@RequiredArgsConstructor
public final class MailboxFolderController {
  private final MailboxRepository mailboxRepository;
  private final MailboxConnection connection;
  private final SecretBox secretBox;

  /**
   * For a saved mailbox an empty password uses the stored one.
   */
  @RequestMapping(path = "/business/mailboxes/folders/",
    method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> findFolders(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var draft = MailboxDraft.parseServer(body);
    var mailboxId = UuidParameter.parse(body.getString("id", 36));
    if (draft.isEmpty()) {
      return ApiResponse.error(6000).future();
    }
    return withPassword(draft.get(), mailboxId)
      .thenCompose(complete -> complete.map(connection::folders)
        .orElseGet(() -> ApiResponse.error(6000).future()));
  }

  private CompletableFuture<Optional<MailboxDraft>> withPassword(
    MailboxDraft draft, Optional<UUID> mailboxId
  ) {
    if (draft.hasPassword()) {
      return CompletableFuture.completedFuture(Optional.of(draft));
    }
    if (mailboxId.isEmpty()) {
      return CompletableFuture.completedFuture(Optional.empty());
    }
    return mailboxRepository.findById(mailboxId.get())
      .thenApply(mailbox -> mailbox.map(entry -> draft.withPassword(
        secretBox.decrypt(entry.encryptedPassword()))));
  }
}