package de.lukasbreuer.clarity.api.web.business;

import de.lukasbreuer.clarity.api.business.mail.MailFolderCursorRepository;
import de.lukasbreuer.clarity.api.business.mail.Mailbox;
import de.lukasbreuer.clarity.api.business.mail.MailboxRepository;
import de.lukasbreuer.clarity.api.secret.SecretBox;
import de.lukasbreuer.clarity.api.space.Space;
import de.lukasbreuer.clarity.api.space.SpaceRepository;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;
import de.lukasbreuer.clarity.api.web.request.UuidParameter;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Connects the IMAP mailboxes invoices arrive in to a business space and
 * changes them. A mailbox is only saved once the api could log in and found
 * its folders; the password is stored encrypted and never sent back. Error
 * codes: 6000 invalid mailbox, 6001 unknown mailbox, 6002 connection or
 * login failed, 6003 folder not found, 5001 unknown or personal space.
 */
@RestController
@RequiredArgsConstructor
public final class MailboxController {
  private final MailboxRepository mailboxRepository;
  private final MailFolderCursorRepository cursorRepository;
  private final SpaceRepository spaceRepository;
  private final MailboxConnection connection;
  private final SecretBox secretBox;

  @RequestMapping(path = "/business/mailboxes/", method = RequestMethod.GET)
  public CompletableFuture<ApiResponse> findMailboxes(
    @RequestParam("space") String space
  ) {
    var spaceId = UuidParameter.parse(space);
    if (spaceId.isEmpty()) {
      return ApiResponse.error(5001).future();
    }
    return mailboxRepository.findBySpaceId(spaceId.get())
      .thenApply(mailboxes -> ApiResponse.success(Map.of("mailboxes",
        mailboxes.stream()
          .sorted(Comparator.comparingLong(Mailbox::createdAt))
          .map(Mailbox::information)
          .toList())));
  }

  @RequestMapping(path = "/business/mailboxes/create/",
    method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> create(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var spaceId = body.getUUID("space");
    var draft = MailboxDraft.parse(body);
    if (spaceId == null) {
      return ApiResponse.error(5001).future();
    }
    if (draft.isEmpty() || !draft.get().hasPassword()) {
      return ApiResponse.error(6000).future();
    }
    return spaceRepository.findById(spaceId)
      .thenCompose(space -> space.filter(Space::isBusiness).isEmpty() ?
        ApiResponse.error(5001).future() : create(spaceId, draft.get()));
  }

  private CompletableFuture<ApiResponse> create(
    UUID spaceId, MailboxDraft draft
  ) {
    return connection.whenVerified(draft, () -> save(draft.create(spaceId,
      secretBox.encrypt(draft.password()))));
  }

  @RequestMapping(path = "/business/mailboxes/update/",
    method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> update(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var mailboxId = body.getUUID("id");
    var draft = MailboxDraft.parse(body);
    if (mailboxId == null) {
      return ApiResponse.error(6001).future();
    }
    if (draft.isEmpty()) {
      return ApiResponse.error(6000).future();
    }
    return mailboxRepository.findById(mailboxId)
      .thenCompose(mailbox -> mailbox.isEmpty() ?
        ApiResponse.error(6001).future() : update(mailbox.get(), draft.get()));
  }

  /**
   * Without a new password the login is tried with the stored one.
   */
  private CompletableFuture<ApiResponse> update(
    Mailbox mailbox, MailboxDraft draft
  ) {
    var complete = draft.hasPassword() ? draft :
      draft.withPassword(secretBox.decrypt(mailbox.encryptedPassword()));
    return connection.whenVerified(complete,
      () -> save(edit(mailbox, draft)));
  }

  /**
   * Other folders are read from the start again, so their cursors go.
   */
  private Mailbox edit(Mailbox mailbox, MailboxDraft draft) {
    if (draft.changesFolders(mailbox)) {
      cursorRepository.deleteByMailboxId(mailbox.id());
    }
    draft.applyTo(mailbox, draft.hasPassword() ?
      secretBox.encrypt(draft.password()) : null);
    return mailbox;
  }

  private CompletableFuture<ApiResponse> save(Mailbox mailbox) {
    return mailboxRepository.save(mailbox)
      .thenApply(saved -> ApiResponse.success(Map.of("mailbox",
        saved.information())));
  }
}