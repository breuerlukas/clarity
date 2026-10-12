package de.lukasbreuer.clarity.api.transaction.category;

import de.lukasbreuer.clarity.api.transaction.Category;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Merchant keywords per category, read from {@code category-keywords.json}.
 * Order matters: the first match wins, so "amazon prime" has to come before
 * "amazon" in the file.
 */
@RequiredArgsConstructor(staticName = "create")
public final class CategoryKeywords {
  private static final String RESOURCE = "/category-keywords.json";

  public static CategoryKeywords load() {
    try (var stream = CategoryKeywords.class.getResourceAsStream(RESOURCE)) {
      if (stream == null) {
        throw new IllegalStateException("Missing " + RESOURCE);
      }
      var categories = new JSONObject(new String(stream.readAllBytes(),
        StandardCharsets.UTF_8)).getJSONArray("categories");
      return create(IntStream.range(0, categories.length())
        .mapToObj(categories::getJSONObject)
        .map(entry -> Map.entry(Category.valueOf(entry.getString("category")),
          keywords(entry.getJSONArray("keywords"))))
        .toList());
    } catch (IOException exception) {
      throw new IllegalStateException("Unreadable " + RESOURCE, exception);
    }
  }

  private static List<String> keywords(JSONArray array) {
    return IntStream.range(0, array.length()).mapToObj(array::getString)
      .toList();
  }

  private final List<Map.Entry<Category, List<String>>> keywords;

  public Optional<Category> find(String text) {
    return keywords.stream()
      .filter(entry -> entry.getValue().stream().anyMatch(keyword ->
        KeywordMatch.of(text, keyword).matches()))
      .map(Map.Entry::getKey)
      .findFirst();
  }
}