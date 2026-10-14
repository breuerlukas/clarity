package de.lukasbreuer.clarity.api.business.mail;

import java.util.Arrays;
import java.util.Optional;

/**
 * How the connection to the mail server is encrypted: TLS from the start
 * (usually port 993) or upgraded with STARTTLS (usually 143). Plain
 * connections are not offered; the password would travel in the clear.
 */
public enum MailSecurity {
  SSL,
  STARTTLS;

  public static Optional<MailSecurity> find(String name) {
    return Arrays.stream(values())
      .filter(security -> security.name().equals(name))
      .findFirst();
  }
}