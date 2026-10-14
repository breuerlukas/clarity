package de.lukasbreuer.clarity.api.secret;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class SecretBoxTest {
  @TempDir
  Path directory;

  @Test
  void decryptsWhatItEncrypted() throws Exception {
    var box = SecretBox.load(directory.resolve("clarity.key"));
    var encrypted = box.encrypt("mailbox password");
    assertNotEquals(encrypted, box.encrypt("mailbox password"));
    assertEquals("mailbox password", box.decrypt(encrypted));
    assertEquals("mailbox password", SecretBox.load(directory
      .resolve("clarity.key")).decrypt(encrypted));
  }

  @Test
  void createsKeyOnlyTheOwnerCanRead() throws Exception {
    var keyFile = directory.resolve("data/clarity.key");
    SecretBox.load(keyFile);
    assertEquals("rw-------", PosixFilePermissions.toString(
      Files.getPosixFilePermissions(keyFile)));
  }

  @Test
  void refusesChangedOrForeignSecrets() throws Exception {
    var box = SecretBox.load(directory.resolve("clarity.key"));
    var other = SecretBox.of(new byte[32]);
    var encrypted = box.encrypt("secret");
    assertThrows(IllegalStateException.class, () -> other.decrypt(encrypted));
    var changed = encrypted.substring(0, encrypted.length() - 4) + "AAA=";
    assertThrows(IllegalStateException.class, () -> box.decrypt(changed));
  }
}