package de.lukasbreuer.clarity.api.secret;

import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * The file that holds the key of the {@link SecretBox}, Base64 encoded. It
 * is created with a new random key on the first start, readable only by the
 * owner where the file system knows permissions.
 */
@RequiredArgsConstructor(staticName = "of")
public final class SecretKeyFile {
  private static final int KEY_BYTES = 32;
  private static final SecureRandom RANDOM = new SecureRandom();

  private final Path keyFile;

  /**
   * @throws IOException If the file cannot be written or holds no 256 bit
   *                     key
   */
  public byte[] readOrCreate() throws IOException {
    if (!Files.exists(keyFile)) {
      create();
    }
    var key = Base64.getDecoder().decode(Files.readString(keyFile).trim());
    if (key.length != KEY_BYTES) {
      throw new IOException("The key in " + keyFile + " is not 256 bit long");
    }
    return key;
  }

  /**
   * Another start may have created the file in the meantime; its key is
   * kept then.
   */
  private void create() throws IOException {
    createDirectories();
    try {
      createOwnerOnly();
    } catch (FileAlreadyExistsException exception) {
      return;
    }
    Files.writeString(keyFile, Base64.getEncoder().encodeToString(newKey()));
  }

  private void createDirectories() throws IOException {
    var parent = keyFile.toAbsolutePath().getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
  }

  /**
   * File systems without POSIX permissions get a plain file.
   */
  private void createOwnerOnly() throws IOException {
    try {
      Files.createFile(keyFile, PosixFilePermissions.asFileAttribute(
        PosixFilePermissions.fromString("rw-------")));
    } catch (UnsupportedOperationException exception) {
      Files.createFile(keyFile);
    }
  }

  private byte[] newKey() {
    var key = new byte[KEY_BYTES];
    RANDOM.nextBytes(key);
    return key;
  }
}