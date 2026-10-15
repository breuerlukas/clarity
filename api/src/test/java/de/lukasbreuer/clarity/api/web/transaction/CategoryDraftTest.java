package de.lukasbreuer.clarity.api.web.transaction;

import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CategoryDraftTest {
  @Test
  void keywordsAreLowerCaseUniqueAndKeepTheirTrailingSpace() {
    var draft = parse("{\"name\": \" Haustiere \", \"icon\": \"paw-print\"," +
      "\"color\": \"teal\", \"kind\": \"SPENDING\", \"keywords\": " +
      "[\"  Fressnapf\", \"fressnapf\", \"\", \"tierarzt \"]}");
    assertEquals("Haustiere", draft.orElseThrow().chosenName());
    assertEquals(List.of("fressnapf", "tierarzt "),
      draft.orElseThrow().keywords());
  }

  @Test
  void anEmptyNameKeepsTheTranslatedLabel() {
    var draft = parse("{\"name\": \"\", \"icon\": \"home\", \"color\": " +
      "\"gray\", \"kind\": \"SPENDING\", \"keywords\": []}");
    assertNull(draft.orElseThrow().chosenName());
  }

  @Test
  void refusesUnknownKindsBadIconsAndSingleLetterKeywords() {
    assertTrue(parse("{\"name\": \"A\", \"icon\": \"home\", \"color\": " +
      "\"gray\", \"kind\": \"BOTH\", \"keywords\": []}").isEmpty());
    assertTrue(parse("{\"name\": \"A\", \"icon\": \"<svg>\", \"color\": " +
      "\"gray\", \"kind\": \"SPENDING\", \"keywords\": []}").isEmpty());
    assertTrue(parse("{\"name\": \"A\", \"icon\": \"home\", \"color\": " +
      "\"gray\", \"kind\": \"SPENDING\", \"keywords\": [\"a \"]}").isEmpty());
  }

  private Optional<CategoryDraft> parse(String payload) {
    return CategoryDraft.parse(ApiRequestBody.of(payload,
      new MockHttpServletResponse()));
  }
}