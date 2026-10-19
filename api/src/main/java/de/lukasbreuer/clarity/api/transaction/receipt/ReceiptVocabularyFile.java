package de.lukasbreuer.clarity.api.transaction.receipt;

import de.lukasbreuer.clarity.api.business.invoice.reading.AmountLabels;
import de.lukasbreuer.clarity.api.business.invoice.reading.LabelPhrases;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * Reads {@code receipt-vocabulary.json} and turns its phrases into a
 * {@link ReceiptVocabulary}.
 */
@RequiredArgsConstructor(staticName = "of")
public final class ReceiptVocabularyFile {
  private static final String RESOURCE = "/receipt-vocabulary.json";
  private static final int FLAGS =
    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;

  public static ReceiptVocabularyFile load() {
    try (var stream = ReceiptVocabularyFile.class
      .getResourceAsStream(RESOURCE)) {
      if (stream == null) {
        throw new IllegalStateException("Missing " + RESOURCE);
      }
      return of(new JSONObject(new String(stream.readAllBytes(),
        StandardCharsets.UTF_8)));
    } catch (IOException exception) {
      throw new IllegalStateException("Unreadable " + RESOURCE, exception);
    }
  }

  private final JSONObject file;

  public ReceiptVocabulary vocabulary() {
    return ReceiptVocabulary.create(AmountLabels.of(phrases("total_labels")),
      words("total_labels"), words("other_lines"));
  }

  private Pattern words(String key) {
    return Pattern.compile("\\b" + phrases(key).anyOf() + "\\b", FLAGS);
  }

  private LabelPhrases phrases(String key) {
    var array = file.getJSONArray(key);
    return LabelPhrases.of(IntStream.range(0, array.length())
      .mapToObj(array::getString).toList());
  }
}