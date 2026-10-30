package de.lukasbreuer.clarity.api.owner.session;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OwnerSessionTest {
  private static final long MINUTE = 60_000;

  @Test
  void survivesTheGapBetweenTwoRequestsOfAnOpenPanel() {
    assertTrue(lastSeenMinutesAgo(4).isValid());
  }

  @Test
  void endsSoonAfterTheLastTabIsGone() {
    assertFalse(lastSeenMinutesAgo(6).isValid());
  }

  private OwnerSession lastSeenMinutesAgo(int minutes) {
    var now = System.currentTimeMillis();
    return OwnerSession.create(UUID.randomUUID(), "hash",
      OwnerSessionStatus.ACTIVE, "127.0.0.1", "", now - 60 * MINUTE,
      now - minutes * MINUTE);
  }
}