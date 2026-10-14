package de.lukasbreuer.clarity.api.web.business;

import de.lukasbreuer.clarity.api.business.mail.MailSecurity;
import de.lukasbreuer.clarity.api.business.mail.Mailbox;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;
import lombok.With;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The connection details of a mailbox as the panel sends them, checked: a
 * host name or address, a port, TLS or STARTTLS, a user name and the folders
 * to read. An empty password means keeping the stored one, and an empty
 * outgoing folder means reading none.
 */
record MailboxDraft(
  String host, int port, MailSecurity security, String username,
  @With String password, String incomingFolder, String outgoingFolder
) {
  private static final String HOST_PATTERN = "[A-Za-z0-9.:\\-\\[\\]]{1,253}";
  private static final int MAXIMUM_FOLDER_LENGTH = 200;

  static Optional<MailboxDraft> parse(ApiRequestBody body) {
    try {
      var outgoing = body.getString("outgoing_folder", MAXIMUM_FOLDER_LENGTH)
        .trim();
      return Optional.of(fromBody(body,
          body.getString("incoming_folder", MAXIMUM_FOLDER_LENGTH).trim(),
          outgoing.isEmpty() ? null : outgoing))
        .filter(MailboxDraft::isValid);
    } catch (Exception exception) {
      return Optional.empty();
    }
  }

  /**
   * For listing folders, where no folder is chosen yet.
   */
  static Optional<MailboxDraft> parseServer(ApiRequestBody body) {
    try {
      return Optional.of(fromBody(body, "INBOX", null))
        .filter(MailboxDraft::isValid);
    } catch (Exception exception) {
      return Optional.empty();
    }
  }

  private static MailboxDraft fromBody(
    ApiRequestBody body, String incomingFolder, String outgoingFolder
  ) {
    return new MailboxDraft(body.getString("host", 253).trim(),
      (int) body.getLong("port"),
      MailSecurity.find(body.getString("security", 16)).orElseThrow(),
      body.getString("username", 200).trim(),
      body.getString("password", 500), incomingFolder, outgoingFolder);
  }

  private boolean isValid() {
    return host.matches(HOST_PATTERN) && port > 0 && port <= 65535 &&
      !username.isEmpty() && !incomingFolder.isEmpty() &&
      !incomingFolder.equals(outgoingFolder);
  }

  boolean hasPassword() {
    return !password.isEmpty();
  }

  /**
   * The folders that have to exist on the server.
   */
  List<String> folders() {
    return outgoingFolder == null ? List.of(incomingFolder) :
      List.of(incomingFolder, outgoingFolder);
  }

  boolean changesFolders(Mailbox mailbox) {
    return !incomingFolder.equals(mailbox.incomingFolder()) ||
      !Objects.equals(outgoingFolder, mailbox.outgoingFolder());
  }

  Mailbox create(UUID spaceId, String encryptedPassword) {
    return Mailbox.create(UUID.randomUUID(), spaceId, host, port, security,
      username, encryptedPassword, incomingFolder, outgoingFolder, null, null,
      System.currentTimeMillis());
  }

  /**
   * @param encryptedPassword Null keeps the stored password
   */
  void applyTo(Mailbox mailbox, String encryptedPassword) {
    mailbox.edit(host, port, security, username, encryptedPassword,
      incomingFolder, outgoingFolder);
  }
}