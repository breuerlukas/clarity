package de.lukasbreuer.clarity.api.business.invoice.matching;

import lombok.RequiredArgsConstructor;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Compares the name on an invoice with the name the bank shows for the
 * other side of a transfer, which is often shortened or written
 * differently: two names are similar when they share a distinctive word,
 * legal forms and filler words aside.
 */
@RequiredArgsConstructor(staticName = "of")
public final class CompanyName {
  private static final Set<String> COMMON = Set.of("gmbh", "mbh", "ug",
    "haftungsbeschrankt", "ag", "kg", "ohg", "gbr", "ek", "e", "k", "co",
    "und", "and", "the", "der", "die", "das", "ltd", "inc", "llc", "se",
    "partg", "eg", "ev", "gesellschaft", "deutschland", "germany", "online",
    "services", "service", "solutions");
  private static final int MINIMUM_WORD_LENGTH = 3;

  private final String name;

  public boolean isSimilarTo(String otherName) {
    var words = words();
    return !words.isEmpty() && CompanyName.of(otherName).words().stream()
      .anyMatch(words::contains);
  }

  private Set<String> words() {
    if (name == null) {
      return Set.of();
    }
    return Arrays.stream(plain().split("[^a-z0-9]+"))
      .filter(this::isDistinctive)
      .collect(Collectors.toSet());
  }

  /**
   * Lower case without accents, as banks often drop them.
   */
  private String plain() {
    return Normalizer.normalize(name, Normalizer.Form.NFD)
      .replaceAll("\\p{M}", "")
      .toLowerCase(Locale.ROOT);
  }

  private boolean isDistinctive(String word) {
    return word.length() >= MINIMUM_WORD_LENGTH && !COMMON.contains(word);
  }
}