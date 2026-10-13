package de.lukasbreuer.clarity.api.banking;

import io.jsonwebtoken.Jwts;
import org.apache.commons.configuration2.INIConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EnableBankingSignerTest {
  @TempDir
  private Path directory;

  @Test
  void signsATokenEnableBankingAccepts() throws Exception {
    var keys = KeyPairGenerator.getInstance("RSA").generateKeyPair();
    var signer = new EnableBankingSigner(configuration(keys));
    var token = Jwts.parser().verifyWith(keys.getPublic()).build()
      .parseSignedClaims(signer.token());
    assertEquals("application", token.getHeader().getKeyId());
    assertEquals("RS256", token.getHeader().getAlgorithm());
    assertEquals("enablebanking.com", token.getPayload().getIssuer());
    assertEquals("api.enablebanking.com",
      token.getPayload().getAudience().iterator().next());
    assertTrue(token.getPayload().getExpiration().getTime() >
      System.currentTimeMillis());
  }

  private BankingConfiguration configuration(KeyPair keys) throws Exception {
    var keyFile = directory.resolve("enable-banking.pem");
    Files.writeString(keyFile, pem("PRIVATE KEY",
      keys.getPrivate().getEncoded()));
    var file = new INIConfiguration();
    file.setProperty("banking.application_id", "application");
    file.setProperty("banking.key_file", keyFile.toString());
    file.setProperty("banking.redirect_url", "http://localhost/");
    var configuration = BankingConfiguration.create();
    configuration.load(file);
    return configuration;
  }

  /**
   * The key is generated for each run. Its PEM frame is put together here so
   * that secret scanners don't mistake the test for a committed key.
   */
  private String pem(String type, byte[] content) {
    var dashes = "-".repeat(5);
    return dashes + "BEGIN " + type + dashes + "\n" +
      Base64.getMimeEncoder().encodeToString(content) + "\n" +
      dashes + "END " + type + dashes + "\n";
  }
}
