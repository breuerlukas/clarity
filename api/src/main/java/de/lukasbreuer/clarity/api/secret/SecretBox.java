package de.lukasbreuer.clarity.api.secret;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Encrypts secrets the api has to read again later, such as mailbox
 * passwords, with AES-GCM. The key lives in its own file next to the api
 * rather than in the database, so a database dump alone reveals none of them.
 * The file is created on the first start; losing it means entering those
 * secrets again.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class SecretBox {
  private static final int KEY_BYTES = 32;
  private static final int NONCE_BYTES = 12;
  private static final int TAG_BITS = 128;
  private static final String TRANSFORMATION = "AES/GCM/NoPadding";
  private static final SecureRandom RANDOM = new SecureRandom();

  public static SecretBox load(Path keyFile) throws IOException {
    if (!Files.exists(keyFile)) {
      create(keyFile);
    }
    var key = Base64.getDecoder().decode(Files.readString(keyFile).trim());
    if (key.length != KEY_BYTES) {
      throw new IOException("The key in " + keyFile + " is not 256 bit long");
    }
    return new SecretBox(new SecretKeySpec(key, "AES"));
  }

  public static SecretBox of(byte[] key) {
    return new SecretBox(new SecretKeySpec(key, "AES"));
  }

  private static void create(Path keyFile) throws IOException {
    var key = new byte[KEY_BYTES];
    RANDOM.nextBytes(key);
    var parent = keyFile.toAbsolutePath().getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    try {
      Files.createFile(keyFile, PosixFilePermissions.asFileAttribute(
        PosixFilePermissions.fromString("rw-------")));
    } catch (FileAlreadyExistsException exception) {
      return;
    } catch (UnsupportedOperationException exception) {
      Files.createFile(keyFile);
    }
    Files.writeString(keyFile, Base64.getEncoder().encodeToString(key));
  }

  private final SecretKey key;

  /**
   * @return The nonce followed by the ciphertext, Base64 encoded
   */
  public String encrypt(String secret) {
    try {
      var nonce = new byte[NONCE_BYTES];
      RANDOM.nextBytes(nonce);
      var cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.ENCRYPT_MODE, key,
        new GCMParameterSpec(TAG_BITS, nonce));
      var ciphertext = cipher.doFinal(secret.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(ByteBuffer
        .allocate(nonce.length + ciphertext.length)
        .put(nonce).put(ciphertext).array());
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException(exception);
    }
  }

  /**
   * @throws IllegalStateException If the value was not encrypted with this
   *                               key or was changed since
   */
  public String decrypt(String encrypted) {
    try {
      var bytes = Base64.getDecoder().decode(encrypted);
      var cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.DECRYPT_MODE, key,
        new GCMParameterSpec(TAG_BITS, bytes, 0, NONCE_BYTES));
      return new String(cipher.doFinal(bytes, NONCE_BYTES,
        bytes.length - NONCE_BYTES), StandardCharsets.UTF_8);
    } catch (GeneralSecurityException | IllegalArgumentException exception) {
      throw new IllegalStateException("The secret cannot be decrypted",
        exception);
    }
  }
}
