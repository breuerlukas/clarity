package de.lukasbreuer.clarity.api.web.business;

import de.lukasbreuer.clarity.api.business.mail.MailServer;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import jakarta.mail.MessagingException;
import jakarta.mail.Store;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Tries the connection details the owner entered against the mail server,
 * off the request thread since a login can take seconds. Error codes: 6002
 * connection or login failed, 6003 folder not found.
 */
@Component
@RequiredArgsConstructor
final class MailboxConnection {
  private final MailServer mailServer;

  /**
   * Runs the action only once the api could log in and found every folder
   * of the draft, and answers with the error code otherwise.
   */
  CompletableFuture<ApiResponse> whenVerified(
    MailboxDraft draft, Supplier<CompletableFuture<ApiResponse>> action
  ) {
    return CompletableFuture.supplyAsync(() -> problem(draft))
      .thenCompose(error -> error.map(code -> ApiResponse.error(code)
        .future()).orElseGet(action));
  }

  CompletableFuture<ApiResponse> folders(MailboxDraft draft) {
    return CompletableFuture.supplyAsync(() -> listFolders(draft));
  }

  private ApiResponse listFolders(MailboxDraft draft) {
    try (var store = connect(draft)) {
      return ApiResponse.success(Map.of("folders",
        mailServer.folders(store)));
    } catch (MessagingException exception) {
      return ApiResponse.error(6002);
    }
  }

  private Optional<Integer> problem(MailboxDraft draft) {
    try (var store = connect(draft)) {
      return hasFolders(store, draft.folders()) ? Optional.empty() :
        Optional.of(6003);
    } catch (MessagingException exception) {
      return Optional.of(6002);
    }
  }

  private boolean hasFolders(Store store, List<String> folders)
    throws MessagingException {
    for (var folder : folders) {
      if (!mailServer.exists(store, folder)) {
        return false;
      }
    }
    return true;
  }

  private Store connect(MailboxDraft draft) throws MessagingException {
    return mailServer.connect(draft.host(), draft.port(), draft.security(),
      draft.username(), draft.password());
  }
}