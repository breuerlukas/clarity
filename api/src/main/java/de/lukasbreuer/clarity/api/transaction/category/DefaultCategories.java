package de.lukasbreuer.clarity.api.transaction.category;

import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * The built-in categories as shipped in {@code default-categories.json}, in
 * the order their keywords are tried: "amazon prime" (subscriptions) has to
 * come before "amazon" (shopping). They seed the database and are what a
 * reset goes back to. Every call builds fresh entities, so a caller can
 * change them without touching the shipped state.
 */
@RequiredArgsConstructor(staticName = "create")
public final class DefaultCategories {
  private static final String RESOURCE = "/default-categories.json";
  private static final String NEUTRAL_COLOR = "gray";

  /**
   * Builds every category once, so a broken file stops the boot instead of
   * failing later.
   */
  public static DefaultCategories load() {
    try (var stream = DefaultCategories.class.getResourceAsStream(RESOURCE)) {
      if (stream == null) {
        throw new IllegalStateException("Missing " + RESOURCE);
      }
      var defaults = create(new JSONObject(new String(stream.readAllBytes(),
        StandardCharsets.UTF_8)).getJSONArray("categories"));
      defaults.all();
      return defaults;
    } catch (IOException exception) {
      throw new IllegalStateException("Unreadable " + RESOURCE, exception);
    }
  }

  private final JSONArray entries;

  public List<Category> all() {
    return IntStream.range(0, entries.length()).mapToObj(this::category)
      .toList();
  }

  public Optional<Category> find(String key) {
    return all().stream().filter(category -> category.key().equals(key))
      .findFirst();
  }

  private Category category(int index) {
    var entry = entries.getJSONObject(index);
    return Category.create(entry.getString("key"), null,
      entry.getString("icon"), NEUTRAL_COLOR,
      CategoryKind.valueOf(entry.getString("kind")),
      keywords(entry.getJSONArray("keywords")), false, true, index);
  }

  private List<String> keywords(JSONArray array) {
    return IntStream.range(0, array.length()).mapToObj(array::getString)
      .toList();
  }
}