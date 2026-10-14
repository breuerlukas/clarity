package de.lukasbreuer.clarity.api.business.invoice.reading;

import lombok.RequiredArgsConstructor;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Reads {@code invoice-vocabulary.json} and turns its phrases into an
 * {@link InvoiceVocabulary}: headings at the start of a line, and labels
 * followed by the number, amount or date they name.
 */
@RequiredArgsConstructor(staticName = "of")
final class InvoiceVocabularyFile {
  private static final String RESOURCE = "/invoice-vocabulary.json";
  private static final int FLAGS = AmountLabels.FLAGS;
  private static final String LABEL_END = "\\.?\\s*[:.]?\\s*";
  private static final String NUMBER =
    "([A-Z0-9][A-Z0-9\\-/_.]{1,30}[A-Z0-9])";
  private static final String TAX_RATE =
    "\\.?(?:[^\\n\\d]{0,20}\\d{1,2}(?:[.,]\\d{1,2})?\\s?%)?";

  static InvoiceVocabularyFile load() {
    try (var stream = InvoiceVocabularyFile.class
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

  InvoiceVocabulary vocabulary() {
    return InvoiceVocabulary.create(lineStart("headings"),
      lineStart("other_documents"),
      labelled("number_labels", LABEL_END + NUMBER),
      AmountLabels.of(phrases("gross_labels")),
      AmountLabels.of(phrases("net_labels")),
      labelled("tax_labels",
        TAX_RATE + AmountLabels.GAP + AmountLabels.AMOUNT),
      labelled("invoice_date_labels", LABEL_END + PrintedDate.PATTERN),
      labelled("due_date_labels", LABEL_END + PrintedDate.PATTERN),
      Pattern.compile(phrases("payment_terms").anyOf(), FLAGS), months());
  }

  private Pattern lineStart(String key) {
    return Pattern.compile("^\\s*" + phrases(key).anyOf() + "\\b",
      FLAGS | Pattern.MULTILINE);
  }

  private Pattern labelled(String key, String value) {
    return Pattern.compile(phrases(key).anyOf() + value, FLAGS);
  }

  private LabelPhrases phrases(String key) {
    var array = file.getJSONArray(key);
    return LabelPhrases.of(IntStream.range(0, array.length())
      .mapToObj(array::getString).toList());
  }

  private Map<String, Integer> months() {
    var months = file.getJSONObject("months");
    return months.keySet().stream()
      .collect(Collectors.toMap(name -> name, months::getInt));
  }
}