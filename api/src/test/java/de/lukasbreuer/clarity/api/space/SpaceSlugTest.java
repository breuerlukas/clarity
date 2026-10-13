package de.lukasbreuer.clarity.api.space;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class SpaceSlugTest {
  @Test
  void spellsOutUmlautsAndDropsPunctuation() {
    assertEquals("cafe-mueller-co",
      SpaceSlug.of(Set.of()).forName("  Café Müller & Co. "));
  }

  @Test
  void numbersTakenSlugs() {
    assertEquals("agentur-3",
      SpaceSlug.of(Set.of("agentur", "agentur-2")).forName("Agentur"));
  }

  @Test
  void fallsBackForNamesWithoutLetters() {
    assertEquals("space", SpaceSlug.of(Set.of()).forName("💼"));
  }
}
