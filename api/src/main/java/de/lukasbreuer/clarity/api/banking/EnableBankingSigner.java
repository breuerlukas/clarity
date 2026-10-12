package de.lukasbreuer.clarity.api.banking;

import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

/**
 * Signs the short-lived JWT that authenticates every Enable Banking request
 * with the application's private key. The key is read from disk on first use,
 * so the api still boots (and setup still works) before banking is set up.
 */
@Component
@RequiredArgsConstructor
public final class EnableBankingSigner {
  private static final long TOKEN_LIFETIME = 1000L * 60 * 60;
  private static final String PKCS1_HEADER = "BEGIN RSA PRIVATE KEY";

  private final BankingConfiguration configuration;
  private PrivateKey privateKey;

  public String token() throws Exception {
    var now = System.currentTimeMillis();
    return Jwts.builder()
      .header().keyId(configuration.applicationId()).type("JWT").and()
      .issuer("enablebanking.com")
      .audience().single("api.enablebanking.com")
      .issuedAt(new Date(now))
      .expiration(new Date(now + TOKEN_LIFETIME))
      .signWith(privateKey(), Jwts.SIG.RS256)
      .compact();
  }

  public boolean isConfigured() {
    return !configuration.applicationId().isBlank() &&
      Files.isReadable(Path.of(configuration.keyFile()));
  }

  private synchronized PrivateKey privateKey() throws Exception {
    if (privateKey == null) {
      privateKey = readPrivateKey(Files.readString(
        Path.of(configuration.keyFile())));
    }
    return privateKey;
  }

  /**
   * Enable Banking hands out PKCS#8 keys. An older PKCS#1 key has to be
   * converted once with
   * {@code openssl pkcs8 -topk8 -nocrypt -in old.pem -out new.pem}.
   */
  private PrivateKey readPrivateKey(String pem) throws Exception {
    if (pem.contains(PKCS1_HEADER)) {
      throw new IllegalStateException("Convert the key to PKCS#8 first");
    }
    var content = pem.replaceAll("-----[A-Z ]+-----", "")
      .replaceAll("\\s", "");
    var specification = new PKCS8EncodedKeySpec(
      Base64.getDecoder().decode(content));
    return KeyFactory.getInstance("RSA").generatePrivate(specification);
  }
}