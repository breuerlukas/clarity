package de.lukasbreuer.clarity.api.business.mail;

import de.lukasbreuer.clarity.api.log.Log;
import jakarta.mail.FetchProfile;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Store;
import jakarta.mail.UIDFolder;
import jakarta.mail.search.ComparisonTerm;
import jakarta.mail.search.ReceivedDateTerm;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
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
@RequiredArgsConstructor(staticName = "create")
public final class MailFolderReader {
  private static final Duration FIRST_READ = Duration.ofDays(90);
  private static final int MAXIMUM_MAILS_PER_RUN = 100;
  private static final int MAXIMUM_MAIL_BYTES = 25 * 1024 * 1024;

  private final Log log;
  private final MailFolderCursorRepository cursorRepository;

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
      read(mailbox, folderName, folder, (UIDFolder) folder, consumer);
    } finally {
      folder.close(false);
    }
  }

  private void read(
    Mailbox mailbox, String folderName, Folder folder, UIDFolder uids,
    Consumer<ReceivedMail> consumer
  ) throws MessagingException {
    var uidValidity = uids.getUIDValidity();
    var cursor = cursorRepository.findByMailboxIdAndFolder(mailbox.id(),
      folderName).join()
      .filter(existing -> existing.isValidFor(uidValidity));
    var lastUid = cursor.map(MailFolderCursor::lastUid).orElse(0L);
    var messages = cursor.isPresent() ?
      uids.getMessagesByUID(lastUid + 1, UIDFolder.MAXUID) :
      folder.search(new ReceivedDateTerm(ComparisonTerm.GE,
        Date.from(Instant.now().minus(FIRST_READ))));
    var profile = new FetchProfile();
    profile.add(UIDFolder.FetchProfileItem.UID);
    profile.add(FetchProfile.Item.SIZE);
    folder.fetch(messages, profile);
    var pending = Arrays.stream(messages)
      .sorted(Comparator.comparingLong(message -> uid(uids, message)))
      .filter(message -> uid(uids, message) > lastUid)
      .limit(MAXIMUM_MAILS_PER_RUN)
      .toList();
    var newestUid = process(uids, pending, consumer).orElse(lastUid);
    if (cursor.isEmpty() && pending.size() < MAXIMUM_MAILS_PER_RUN) {
      newestUid = Math.max(newestUid, uids.getUIDNext() - 1);
    }
    save(mailbox, folderName, uidValidity, newestUid);
  }

  /**
   * @return The UID of the last mail handed on
   */
  private Optional<Long> process(
    UIDFolder uids, List<Message> messages, Consumer<ReceivedMail> consumer
  ) {
    Optional<Long> newest = Optional.empty();
    for (var message : messages) {
      var uid = uid(uids, message);
      try {
        if (message.getSize() <= MAXIMUM_MAIL_BYTES) {
          consumer.accept(MailParser.parse(message));
        }
      } catch (MessagingException | IOException | RuntimeException exception) {
        log.warning("Skipped mail " + uid + ": " + exception.getMessage());
      }
      newest = Optional.of(uid);
    }
    return newest;
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

  private static long uid(UIDFolder folder, Message message) {
    try {
      return folder.getUID(message);
    } catch (MessagingException exception) {
      return -1;
    }
  }
}
