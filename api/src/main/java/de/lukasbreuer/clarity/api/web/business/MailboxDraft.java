package de.lukasbreuer.clarity.api.web.business;

import de.lukasbreuer.clarity.api.business.mail.MailSecurity;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;

import java.util.Optional;

/**
 * The connection details of a mailbox as the panel sends them, checked: a
 * host name or address, a port, TLS or STARTTLS, a user name and the folders
 * to read. An empty password means keeping the stored one, and an empty
 * outgoing folder means reading none.
 */
record MailboxDraft(
  String host, int port, MailSecurity security, String username,
  String password, String incomingFolder, String outgoingFolder
) {
  private static final String HOST_PATTERN = "[A-Za-z0-9.:\\-\\[\\]]{1,253}";
  private static final int MAXIMUM_FOLDER_LENGTH = 200;

  static Optional<MailboxDraft> parse(ApiRequestBody body) {
    try {
      var outgoing = body.getString("outgoing_folder", MAXIMUM_FOLDER_LENGTH)
        .trim();
      var draft = new MailboxDraft(body.getString("host", 253).trim(),
        (int) body.getLong("port"),
        MailSecurity.find(body.getString("security", 16)).orElseThrow(),
        body.getString("username", 200).trim(),
        body.getString("password", 500),
        body.getString("incoming_folder", MAXIMUM_FOLDER_LENGTH).trim(),
        outgoing.isEmpty() ? null : outgoing);
      return draft.isValid() ? Optional.of(draft) : Optional.empty();
    } catch (Exception exception) {
      return Optional.empty();
    }
  }

  /**
   * For listing folders, where no folder is chosen yet.
   */
  static Optional<MailboxDraft> parseServer(ApiRequestBody body) {
    try {
      var draft = new MailboxDraft(body.getString("host", 253).trim(),
        (int) body.getLong("port"),
        MailSecurity.find(body.getString("security", 16)).orElseThrow(),
        body.getString("username", 200).trim(),
        body.getString("password", 500), "INBOX", null);
      return draft.isValid() ? Optional.of(draft) : Optional.empty();
    } catch (Exception exception) {
      return Optional.empty();
    }
  }

  private boolean isValid() {
    return host.matches(HOST_PATTERN) && port > 0 && port <= 65535 &&
      !username.isEmpty() && !incomingFolder.isEmpty() &&
      !incomingFolder.equals(outgoingFolder);
  }

  boolean hasPassword() {
    return !password.isEmpty();
  }

  MailboxDraft withPassword(String password) {
    return new MailboxDraft(host, port, security, username, password,
      incomingFolder, outgoingFolder);
  }
}
