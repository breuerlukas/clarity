package de.lukasbreuer.clarity.api.owner.totp;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * Time-based one-time codes as every authenticator app understands them:
 * SHA-1, six digits, 30 seconds. SHA-256 would be stronger on paper, but
 * Google Authenticator silently ignores it and then produces wrong codes.
 */
@Component
public final class TotpAuthenticator {
  private static final String ISSUER = "Clarity";

  private final DefaultCodeVerifier verifier = new DefaultCodeVerifier(
    new DefaultCodeGenerator(HashingAlgorithm.SHA1), new SystemTimeProvider());

  public String generateSecret() {
    return new DefaultSecretGenerator(32).generate();
  }

  public boolean verify(String secret, String code) {
    return code.matches("\\d{6}") && verifier.isValidCode(secret, code);
  }

  public String uri(String name, String secret) {
    return qrData(name, secret).getUri();
  }

  /**
   * Renders the setup QR code as a data URI, so the browser never has to load
   * the secret from anywhere but this one response.
   */
  public String qrCode(String name, String secret) throws Exception {
    var image = new ZxingPngQrGenerator().generate(qrData(name, secret));
    return "data:image/png;base64," +
      Base64.getEncoder().encodeToString(image);
  }

  private QrData qrData(String name, String secret) {
    return new QrData.Builder()
      .label(name)
      .secret(secret)
      .issuer(ISSUER)
      .algorithm(HashingAlgorithm.SHA1)
      .digits(6)
      .period(30)
      .build();
  }
}