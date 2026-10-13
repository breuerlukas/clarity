package de.lukasbreuer.clarity.api.business.mail;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import de.lukasbreuer.clarity.api.log.Log;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
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
  }

  @AfterEach
  void stop() throws Exception {
    store.close();
    server.stop();
  }

  @Test
  void readsEachMailOnceWithoutMarkingIt() throws Exception {
    deliver("Rechnung 1");
    deliver("Rechnung 2");
    var reader = MailFolderReader.create(Log.create("Test"), cursors());

    assertEquals(List.of("Rechnung 1", "Rechnung 2"), read(reader));
    assertEquals(List.of(), read(reader));
    deliver("Rechnung 3");
    assertEquals(List.of("Rechnung 3"), read(reader));

    var inbox = store.getFolder("INBOX");
    inbox.open(Folder.READ_ONLY);
    for (var message : inbox.getMessages()) {
      assertFalse(message.isSet(Flags.Flag.SEEN));
    }
    inbox.close(false);
  }

  private List<String> read(MailFolderReader reader) throws Exception {
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
      (proxy, method, arguments) -> switch (method.getName()) {
        case "findByMailboxIdAndFolder" -> CompletableFuture.completedFuture(
          Optional.ofNullable(cursors.isEmpty() ? null : cursors.getFirst()));
        case "save" -> {
          cursors.clear();
          cursors.add((MailFolderCursor) arguments[0]);
          yield CompletableFuture.completedFuture(arguments[0]);
        }
        default -> throw new UnsupportedOperationException(method.getName());
      });
  }
}
