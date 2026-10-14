package de.lukasbreuer.clarity.api.business.mail;

import de.lukasbreuer.clarity.api.business.invoice.InvoiceDirection;
import de.lukasbreuer.clarity.api.business.invoice.matching.InvoiceMatcher;
import de.lukasbreuer.clarity.api.log.Log;
import de.lukasbreuer.clarity.api.secret.SecretBox;
import de.lukasbreuer.clarity.api.space.Space;
import de.lukasbreuer.clarity.api.space.SpaceRepository;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.FolderNotFoundException;
import jakarta.mail.MessagingException;
import jakarta.mail.Store;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Fetches new mails of every mailbox every 15 minutes, or of one space when
 * the owner asks, and matches payments afterwards. The sent folder is read
 * first, so a payment advice finds the invoice it announces even when both
 * arrived since the last check. Only one check runs at a time. A failing
 * mailbox records why on itself and the others go on.
 */
@Component
@RequiredArgsConstructor
public final class MailCheck {
  private final Log log;
  private final MailboxRepository mailboxRepository;
  private final SpaceRepository spaceRepository;
  private final MailServer mailServer;
  private final MailFolderReader folderReader;
  private final MailImport mailImport;
  private final InvoiceMatcher invoiceMatcher;
  private final SecretBox secretBox;
  private final AtomicBoolean running = new AtomicBoolean(false);

  @Scheduled(fixedDelay = 15, initialDelay = 2, timeUnit = TimeUnit.MINUTES)
  void checkAll() {
    if (running.compareAndSet(false, true)) {
      checkAndRelease(mailboxRepository::findAll);
    }
  }

  /**
   * @return False if a check is running already
   */
  public boolean checkNow(UUID spaceId) {
    if (!running.compareAndSet(false, true)) {
      return false;
    }
    CompletableFuture.runAsync(() ->
      checkAndRelease(() -> mailboxRepository.findBySpaceId(spaceId)));
    return true;
  }

  private void checkAndRelease(
    Supplier<CompletableFuture<List<Mailbox>>> mailboxes
  ) {
    try {
      mailboxes.get().join().forEach(this::checkSafely);
    } finally {
      running.set(false);
    }
  }

  private void checkSafely(Mailbox mailbox) {
    try {
      spaceRepository.findById(mailbox.spaceId()).join()
        .filter(Space::isBusiness)
        .ifPresent(space -> check(space, mailbox));
    } catch (RuntimeException exception) {
      log.processError(exception);
    }
  }

  private void check(Space space, Mailbox mailbox) {
    try (var store = connect(mailbox)) {
      readFolders(store, space, mailbox);
      mailbox.markChecked();
    } catch (MessagingException | IllegalStateException exception) {
      mailbox.markFailed(failureReason(exception));
    }
    mailboxRepository.save(mailbox).join();
    invoiceMatcher.match(space.id());
  }

  /**
   * @throws IllegalStateException If the stored password cannot be decrypted
   */
  private Store connect(Mailbox mailbox) throws MessagingException {
    return mailServer.connect(mailbox.host(), mailbox.port(),
      mailbox.security(), mailbox.username(),
      secretBox.decrypt(mailbox.encryptedPassword()));
  }

  private void readFolders(Store store, Space space, Mailbox mailbox)
    throws MessagingException {
    if (mailbox.outgoingFolder() != null) {
      folderReader.read(store, mailbox, mailbox.outgoingFolder(), mail ->
        mailImport.store(space, mailbox, InvoiceDirection.OUTGOING, mail));
    }
    folderReader.read(store, mailbox, mailbox.incomingFolder(), mail ->
      mailImport.store(space, mailbox, InvoiceDirection.INCOMING, mail));
  }

  /**
   * Short reasons the panel can show; the server's own message is often
   * long and technical.
   */
  private String failureReason(Exception exception) {
    if (exception instanceof AuthenticationFailedException) {
      return "LOGIN";
    }
    if (exception instanceof FolderNotFoundException) {
      return "FOLDER";
    }
    if (exception instanceof IllegalStateException) {
      return "PASSWORD";
    }
    return "CONNECTION";
  }
}