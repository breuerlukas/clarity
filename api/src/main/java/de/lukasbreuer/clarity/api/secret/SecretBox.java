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
import java.nio.file.Path;
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
  private static final int NONCE_BYTES = 12;
  private static final int TAG_BITS = 128;
  private static final String TRANSFORMATION = "AES/GCM/NoPadding";
  private static final SecureRandom RANDOM = new SecureRandom();

  public static SecretBox load(Path keyFile) throws IOException {
    return of(SecretKeyFile.of(keyFile).readOrCreate());
  }

  public static SecretBox of(byte[] key) {
    return new SecretBox(new SecretKeySpec(key, "AES"));
  }

  private final SecretKey key;

  /**
   * @return The nonce followed by the ciphertext, Base64 encoded
   */
  public String encrypt(String secret) {
    try {
      var nonce = newNonce();
      var ciphertext = cipher(Cipher.ENCRYPT_MODE,
        new GCMParameterSpec(TAG_BITS, nonce))
        .doFinal(secret.getBytes(StandardCharsets.UTF_8));
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
      var plaintext = cipher(Cipher.DECRYPT_MODE,
        new GCMParameterSpec(TAG_BITS, bytes, 0, NONCE_BYTES))
        .doFinal(bytes, NONCE_BYTES, bytes.length - NONCE_BYTES);
      return new String(plaintext, StandardCharsets.UTF_8);
    } catch (GeneralSecurityException | IllegalArgumentException exception) {
      throw new IllegalStateException("The secret cannot be decrypted",
        exception);
    }
  }

  private Cipher cipher(int mode, GCMParameterSpec parameters)
    throws GeneralSecurityException {
    var cipher = Cipher.getInstance(TRANSFORMATION);
    cipher.init(mode, key, parameters);
    return cipher;
  }

  private byte[] newNonce() {
    var nonce = new byte[NONCE_BYTES];
    RANDOM.nextBytes(nonce);
    return nonce;
  }
}