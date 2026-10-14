package de.lukasbreuer.clarity.api.business.mail;

import de.lukasbreuer.clarity.api.log.Log;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Store;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Reads the new mails of one folder, read-only: the folder is opened with
 * EXAMINE, so nothing is marked as seen, moved or deleted. The first read
 * goes back 90 days; later ones take the mails after the cursor, a limited
 * number per run, so a full mailbox is worked through over several runs.
 */
@Component
@RequiredArgsConstructor(staticName = "create")
public final class MailFolderReader {
  private static final Duration FIRST_READ = Duration.ofDays(90);
  private static final int MAXIMUM_MAILS_PER_RUN = 100;
  private static final int MAXIMUM_MAIL_BYTES = 25 * 1024 * 1024;

  private final Log log;
  private final MailFolderCursorRepository cursorRepository;
  private final MailParser mailParser;

  /**
   * @throws jakarta.mail.FolderNotFoundException If the folder is gone
   */
  public void read(
    Store store, Mailbox mailbox, String folderName,
    Consumer<ReceivedMail> consumer
  ) throws MessagingException {
    var folder = store.getFolder(folderName);
    folder.open(Folder.READ_ONLY);
    try {
      read(mailbox, folderName, OpenMailFolder.of(folder), consumer);
    } finally {
      folder.close(false);
    }
  }

  /**
   * A first read that got through every mail moves the cursor to the end of
   * the folder, so older mails beyond the 90 days are never fetched.
   */
  private void read(
    Mailbox mailbox, String folderName, OpenMailFolder folder,
    Consumer<ReceivedMail> consumer
  ) throws MessagingException {
    var uidValidity = folder.uidValidity();
    var lastUid = lastUid(mailbox, folderName, uidValidity);
    var pending = folder.pending(lastUid, Instant.now().minus(FIRST_READ),
      MAXIMUM_MAILS_PER_RUN);
    var newestUid = process(folder, pending, consumer)
      .orElse(lastUid.orElse(0L));
    if (lastUid.isEmpty() && pending.size() < MAXIMUM_MAILS_PER_RUN) {
      newestUid = Math.max(newestUid, folder.newestUid());
    }
    save(mailbox, folderName, uidValidity, newestUid);
  }

  /**
   * Empty if the folder was never read or its UIDs changed since.
   */
  private Optional<Long> lastUid(
    Mailbox mailbox, String folderName, long uidValidity
  ) {
    return cursorRepository.findByMailboxIdAndFolder(mailbox.id(), folderName)
      .join()
      .filter(cursor -> cursor.isValidFor(uidValidity))
      .map(MailFolderCursor::lastUid);
  }

  /**
   * @return The UID of the last mail handed on
   */
  private Optional<Long> process(
    OpenMailFolder folder, List<Message> messages,
    Consumer<ReceivedMail> consumer
  ) {
    messages.forEach(message -> handOn(folder, message, consumer));
    return messages.isEmpty() ? Optional.empty() :
      Optional.of(folder.uid(messages.getLast()));
  }

  /**
   * A mail that cannot be read is skipped rather than blocking the folder.
   */
  private void handOn(
    OpenMailFolder folder, Message message, Consumer<ReceivedMail> consumer
  ) {
    try {
      if (message.getSize() <= MAXIMUM_MAIL_BYTES) {
        consumer.accept(mailParser.parse(message));
      }
    } catch (MessagingException | IOException | RuntimeException exception) {
      log.warning("Skipped mail " + folder.uid(message) + ": " +
        exception.getMessage());
    }
  }

  private void save(
    Mailbox mailbox, String folderName, long uidValidity, long lastUid
  ) {
    var cursor = cursorRepository.findByMailboxIdAndFolder(mailbox.id(),
      folderName).join().orElseGet(() -> MailFolderCursor.create(
        UUID.randomUUID(), mailbox.id(), folderName, uidValidity, 0));
    cursor.advance(uidValidity, lastUid);
    cursorRepository.save(cursor).join();
  }
}