package de.lukasbreuer.clarity.api.web.transaction;

import de.lukasbreuer.clarity.api.transaction.category.Category;
import de.lukasbreuer.clarity.api.transaction.category.CategoryKind;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * The fields of a category as the panel sends them, checked: a name of at
 * most 40 characters (empty keeps a built-in category's translated label),
 * an icon and a color by their short names, and up to 200 keywords. Keywords
 * are lower case and unique; leading spaces go, trailing ones stay, since a
 * keyword matches at the start of a word and "atm " is how one asks for the
 * word to end there.
 */
record CategoryDraft(
  String name, String icon, String color, CategoryKind kind,
  List<String> rawKeywords
) {
  private static final Pattern TOKEN = Pattern.compile("[a-z0-9-]{1,32}");
  private static final int MAXIMUM_NAME_LENGTH = 40;
  private static final int MAXIMUM_KEYWORDS = 200;
  private static final int MINIMUM_KEYWORD_LENGTH = 2;
  private static final int MAXIMUM_KEYWORD_LENGTH = 60;

  static Optional<CategoryDraft> parse(ApiRequestBody body) {
    try {
      var draft = new CategoryDraft(
        body.getString("name", MAXIMUM_NAME_LENGTH).strip(),
        body.getString("icon", 32), body.getString("color", 32),
        CategoryKind.find(body.getString("kind", 16)).orElseThrow(),
        body.getStringList("keywords"));
      return draft.isValid() ? Optional.of(draft) : Optional.empty();
    } catch (Exception exception) {
      return Optional.empty();
    }
  }

  /**
   * @return The name, or null to keep the translated label
   */
  String chosenName() {
    return name.isEmpty() ? null : name;
  }

  List<String> keywords() {
    return rawKeywords.stream()
      .map(keyword -> keyword.stripLeading().toLowerCase(Locale.ROOT))
      .filter(keyword -> !keyword.isBlank())
      .distinct()
      .toList();
  }

  Category create(int position) {
    return Category.create(UUID.randomUUID().toString(), chosenName(), icon,
      color, kind, keywords(), false, false, position);
  }

  void applyTo(Category category) {
    category.edit(chosenName(), icon, color, kind, keywords());
  }

  private boolean isValid() {
    return TOKEN.matcher(icon).matches() && TOKEN.matcher(color).matches() &&
      keywords().size() <= MAXIMUM_KEYWORDS &&
      keywords().stream().allMatch(this::isValidKeyword);
  }

  private boolean isValidKeyword(String keyword) {
    return keyword.strip().length() >= MINIMUM_KEYWORD_LENGTH &&
      keyword.length() <= MAXIMUM_KEYWORD_LENGTH &&
      keyword.chars().noneMatch(Character::isISOControl);
  }
}