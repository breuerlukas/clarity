package de.lukasbreuer.clarity.api.owner;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

/**
 * Locks an address out for 15 minutes after five failed attempts. Applies to
 * the login and to the setup code alike, which turns guessing either into a
 * matter of centuries.
 */
@Component
public final class LoginThrottle {
  private static final int MAXIMUM_FAILURES = 5;
  private static final long WINDOW = 1000L * 60 * 15;

  private final Map<String, ConcurrentLinkedDeque<Long>> failures =
    new ConcurrentHashMap<>();

  public boolean isBlocked(String address) {
    var attempts = failures.get(address);
    return attempts != null && attempts.stream()
      .filter(this::isRecent).count() >= MAXIMUM_FAILURES;
  }

  public void recordFailure(String address) {
    failures.computeIfAbsent(address, _ -> new ConcurrentLinkedDeque<>())
      .add(System.currentTimeMillis());
  }

  public void reset(String address) {
    failures.remove(address);
  }

  @Scheduled(fixedRate = 15, timeUnit = TimeUnit.MINUTES)
  void forgetOldFailures() {
    failures.values().forEach(attempts -> attempts.removeIf(
      attempt -> !isRecent(attempt)));
    failures.values().removeIf(ConcurrentLinkedDeque::isEmpty);
  }

  private boolean isRecent(long attempt) {
    return System.currentTimeMillis() - attempt < WINDOW;
  }
}