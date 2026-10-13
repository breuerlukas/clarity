package de.lukasbreuer.clarity.api.web.business;

import de.lukasbreuer.clarity.api.business.mail.MailCheck;
import de.lukasbreuer.clarity.api.business.mail.MailFolderCursorRepository;
import de.lukasbreuer.clarity.api.business.mail.MailServer;
import de.lukasbreuer.clarity.api.business.mail.Mailbox;
import de.lukasbreuer.clarity.api.business.mail.MailboxRepository;
import de.lukasbreuer.clarity.api.secret.SecretBox;
import de.lukasbreuer.clarity.api.space.Space;
import de.lukasbreuer.clarity.api.space.SpaceRepository;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;
import de.lukasbreuer.clarity.api.web.request.UuidParameter;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import jakarta.mail.MessagingException;
import jakarta.mail.Store;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Connects the IMAP mailboxes invoices arrive in to a business space. A
 * mailbox is only saved once the api could log in and found its folders;
 * the password is stored encrypted and never sent back. Error codes: 6000
 * invalid mailbox, 6001 unknown mailbox, 6002 connection or login failed,
 * 6003 folder not found, 5001 unknown or personal space.
 */
@RestController
@RequiredArgsConstructor
public final class MailboxController {
  private final MailboxRepository mailboxRepository;
  private final MailFolderCursorRepository cursorRepository;
  private final SpaceRepository spaceRepository;
  private final MailCheck mailCheck;
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

  /**
   * Logs in with the given details and lists the folders, so the panel can
   * offer them. For a saved mailbox an empty password uses the stored one.
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
      .thenCompose(complete -> complete.isEmpty() ?
        ApiResponse.error(6000).future() :
        CompletableFuture.supplyAsync(() -> folders(complete.get())));
  }

  private ApiResponse folders(MailboxDraft draft) {
    try (var store = connect(draft)) {
      return ApiResponse.success(Map.of("folders",
        MailServer.folders(store)));
    } catch (MessagingException exception) {
      return ApiResponse.error(6002);
    }
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
        ApiResponse.error(5001).future() :
        CompletableFuture.supplyAsync(() -> verify(draft.get()))
          .thenCompose(error -> error.isPresent() ?
            ApiResponse.error(error.get()).future() :
            save(Mailbox.create(UUID.randomUUID(), spaceId,
              draft.get().host(), draft.get().port(), draft.get().security(),
              draft.get().username(), secretBox.encrypt(draft.get().password()),
              draft.get().incomingFolder(), draft.get().outgoingFolder(), null,
              null, System.currentTimeMillis()))));
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

  private CompletableFuture<ApiResponse> update(
    Mailbox mailbox, MailboxDraft draft
  ) {
    var complete = draft.hasPassword() ? draft :
      draft.withPassword(secretBox.decrypt(mailbox.encryptedPassword()));
    return CompletableFuture.supplyAsync(() -> verify(complete))
      .thenCompose(error -> {
        if (error.isPresent()) {
          return ApiResponse.error(error.get()).future();
        }
        var foldersChanged =
          !draft.incomingFolder().equals(mailbox.incomingFolder()) ||
            !Objects.equals(draft.outgoingFolder(),
              mailbox.outgoingFolder());
        mailbox.edit(draft.host(), draft.port(), draft.security(),
          draft.username(), draft.hasPassword() ?
            secretBox.encrypt(draft.password()) : null,
          draft.incomingFolder(), draft.outgoingFolder());
        if (foldersChanged) {
          cursorRepository.deleteByMailboxId(mailbox.id());
        }
        return save(mailbox);
      });
  }

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
    return mailboxRepository.findById(mailboxId)
      .thenCompose(mailbox -> {
        if (mailbox.isEmpty()) {
          return ApiResponse.error(6001).future();
        }
        cursorRepository.deleteByMailboxId(mailbox.get().id());
        return mailboxRepository.delete(mailbox.get())
          .thenApply(_ -> ApiResponse.success());
      });
  }

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

  private CompletableFuture<ApiResponse> save(Mailbox mailbox) {
    return mailboxRepository.save(mailbox)
      .thenApply(saved -> ApiResponse.success(Map.of("mailbox",
        saved.information())));
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

  /**
   * @return The error code, if the login failed or a folder is missing
   */
  private Optional<Integer> verify(MailboxDraft draft) {
    try (var store = connect(draft)) {
      var folders = draft.outgoingFolder() == null ?
        List.of(draft.incomingFolder()) :
        List.of(draft.incomingFolder(), draft.outgoingFolder());
      for (var folder : folders) {
        if (!MailServer.exists(store, folder)) {
          return Optional.of(6003);
        }
      }
      return Optional.empty();
    } catch (MessagingException exception) {
      return Optional.of(6002);
    }
  }

  private Store connect(MailboxDraft draft)
    throws MessagingException {
    return MailServer.connect(draft.host(), draft.port(), draft.security(),
      draft.username(), draft.password());
  }
}
