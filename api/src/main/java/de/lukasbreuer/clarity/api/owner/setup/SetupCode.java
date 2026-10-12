package de.lukasbreuer.clarity.api.owner.setup;

import de.lukasbreuer.clarity.api.log.Log;
import de.lukasbreuer.clarity.api.owner.OwnerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A one-time code that only appears in the server log. Whoever creates the
 * owner account has to know it, which proves they can read the logs of the
 * machine Clarity runs on. Without it, the first stranger to open a fresh
 * instance could claim it.
 */
@Component
@RequiredArgsConstructor
public final class SetupCode {
  private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  private static final int LENGTH = 12;

  private final Log log;
  private final OwnerRepository ownerRepository;
  private final SecureRandom random = new SecureRandom();
  private final String code = IntStream.range(0, LENGTH)
    .mapToObj(position -> String.valueOf(ALPHABET.charAt(
      random.nextInt(ALPHABET.length()))))
    .collect(Collectors.joining());

  /**
   * Compares in constant time, so the response time does not leak how many
   * leading characters were right.
   */
  public boolean matches(String input) {
    var normalized = input.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
    return MessageDigest.isEqual(normalized.getBytes(StandardCharsets.UTF_8),
      code.getBytes(StandardCharsets.UTF_8));
  }

  @EventListener(ApplicationReadyEvent.class)
  void announce() {
    ownerRepository.existsByConfirmed(true).thenAccept(this::announce);
  }

  private void announce(boolean ownerExists) {
    if (ownerExists) {
      return;
    }
    log.info("No owner yet. Open Clarity and enter this setup code: " +
      readableCode());
  }

  private String readableCode() {
    return code.substring(0, 4) + "-" + code.substring(4, 8) + "-" +
      code.substring(8);
  }
}