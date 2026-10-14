package de.lukasbreuer.clarity.api.business.mail;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import de.lukasbreuer.clarity.api.log.Log;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Reads from a real IMAP server running in memory.
 */
final class MailFolderReaderTest {
  private GreenMail server;
  private Store store;
  private MailFolderReader reader;
  private final List<MailFolderCursor> cursors = new ArrayList<>();
  private final Mailbox mailbox = Mailbox.create(UUID.randomUUID(),
    UUID.randomUUID(), "localhost", 3143, MailSecurity.STARTTLS, "office",
    "", "INBOX", null, null, null, 0);

  @BeforeEach
  void start() throws Exception {
    server = new GreenMail(ServerSetupTest.IMAP);
    server.start();
    server.setUser("office@breuer.example", "office", "secret");
    store = Session.getInstance(new Properties()).getStore("imap");
    store.connect("localhost", ServerSetupTest.IMAP.getPort(), "office",
      "secret");
    reader = MailFolderReader.create(Log.create("Test"), cursors(),
      MailParser.create());
  }

  @AfterEach
  void stop() throws Exception {
    store.close();
    server.stop();
  }

  @Test
  void readsEachMailOnce() throws Exception {
    deliver("Rechnung 1");
    deliver("Rechnung 2");
    assertEquals(List.of("Rechnung 1", "Rechnung 2"), read());
    assertEquals(List.of(), read());
    deliver("Rechnung 3");
    assertEquals(List.of("Rechnung 3"), read());
  }

  @Test
  void leavesMailsUnread() throws Exception {
    deliver("Rechnung 1");
    read();
    var inbox = store.getFolder("INBOX");
    inbox.open(Folder.READ_ONLY);
    var messages = Arrays.asList(inbox.getMessages());
    assertFalse(messages.stream().anyMatch(this::isSeen));
    inbox.close(false);
  }

  private boolean isSeen(Message message) {
    try {
      return message.isSet(Flags.Flag.SEEN);
    } catch (Exception exception) {
      throw new IllegalStateException(exception);
    }
  }

  private List<String> read() throws Exception {
    var subjects = new ArrayList<String>();
    reader.read(store, mailbox, "INBOX", mail -> subjects.add(mail.subject()));
    return subjects;
  }

  private void deliver(String subject) throws Exception {
    var message = new MimeMessage(Session.getInstance(new Properties()));
    message.setFrom(new InternetAddress("billing@weber.example"));
    message.setSubject(subject);
    message.setText("Anbei die Rechnung.");
    message.setSentDate(new Date());
    server.getUserManager().getUser("office").deliver(message);
  }

  /**
   * Keeps the cursors in a list instead of the database.
   */
  private MailFolderCursorRepository cursors() {
    return (MailFolderCursorRepository) Proxy.newProxyInstance(
      getClass().getClassLoader(),
      new Class<?>[] {MailFolderCursorRepository.class},
      (_, method, arguments) -> answer(method, arguments));
  }

  private Object answer(Method method, Object[] arguments) {
    return switch (method.getName()) {
      case "findByMailboxIdAndFolder" -> CompletableFuture.completedFuture(
        Optional.ofNullable(cursors.isEmpty() ? null : cursors.getFirst()));
      case "save" -> save((MailFolderCursor) arguments[0]);
      default -> throw new UnsupportedOperationException(method.getName());
    };
  }

  private CompletableFuture<MailFolderCursor> save(MailFolderCursor cursor) {
    cursors.clear();
    cursors.add(cursor);
    return CompletableFuture.completedFuture(cursor);
  }
}