package de.lukasbreuer.clarity.api.business.invoice.matching;

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
public final class CompanyName {
  private static final Set<String> COMMON = Set.of("gmbh", "mbh", "ug",
    "haftungsbeschrankt", "ag", "kg", "ohg", "gbr", "ek", "e", "k", "co",
    "und", "and", "the", "der", "die", "das", "ltd", "inc", "llc", "se",
    "partg", "eg", "ev", "gesellschaft", "deutschland", "germany", "online",
    "services", "service", "solutions");
  private static final int MINIMUM_WORD_LENGTH = 3;

  private CompanyName() {
  }

  public static boolean isSimilar(String first, String second) {
    var firstWords = words(first);
    return !firstWords.isEmpty() && words(second).stream()
      .anyMatch(firstWords::contains);
  }

  static Set<String> words(String name) {
    if (name == null) {
      return Set.of();
    }
    var plain = Normalizer.normalize(name, Normalizer.Form.NFD)
      .replaceAll("\\p{M}", "")
      .toLowerCase(Locale.ROOT);
    return Arrays.stream(plain.split("[^a-z0-9]+"))
      .filter(word -> word.length() >= MINIMUM_WORD_LENGTH)
      .filter(word -> !COMMON.contains(word))
      .collect(Collectors.toSet());
  }
}
