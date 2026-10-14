package de.lukasbreuer.clarity.api.business.mail;

import jakarta.mail.FetchProfile;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.UIDFolder;
import jakarta.mail.search.ComparisonTerm;
import jakarta.mail.search.ReceivedDateTerm;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * An IMAP folder opened for reading, seen through the UIDs of its mails.
 */
@RequiredArgsConstructor(staticName = "of")
final class OpenMailFolder {
  private final Folder folder;

  long uidValidity() throws MessagingException {
    return uids().getUIDValidity();
  }

  /**
   * The UID the newest mail has, or would have if the folder is empty.
   */
  long newestUid() throws MessagingException {
    return uids().getUIDNext() - 1;
  }

  /**
   * @param lastUid The last mail read, none to start from {@code since}
   * @return The mails after it, oldest first, at most {@code limit}
   */
  List<Message> pending(Optional<Long> lastUid, Instant since, int limit)
    throws MessagingException {
    var messages = lastUid.isPresent() ?
      uids().getMessagesByUID(lastUid.get() + 1, UIDFolder.MAXUID) :
      folder.search(new ReceivedDateTerm(ComparisonTerm.GE,
        Date.from(since)));
    fetchUids(messages);
    return Arrays.stream(messages)
      .sorted(Comparator.comparingLong(this::uid))
      .filter(message -> uid(message) > lastUid.orElse(0L))
      .limit(limit)
      .toList();
  }

  /**
   * @return -1 if the server cannot tell
   */
  long uid(Message message) {
    try {
      return uids().getUID(message);
    } catch (MessagingException exception) {
      return -1;
    }
  }

  /**
   * Loads the UIDs and sizes of all mails in one request instead of one
   * request per mail.
   */
  private void fetchUids(Message[] messages) throws MessagingException {
    var profile = new FetchProfile();
    profile.add(UIDFolder.FetchProfileItem.UID);
    profile.add(FetchProfile.Item.SIZE);
    folder.fetch(messages, profile);
  }

  private UIDFolder uids() {
    return (UIDFolder) folder;
  }
}