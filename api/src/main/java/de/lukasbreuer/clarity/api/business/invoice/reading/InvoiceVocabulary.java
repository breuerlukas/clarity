package de.lukasbreuer.clarity.api.business.invoice.reading;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * What invoices print in German and English, compiled into the expressions
 * {@link InvoiceText} looks for. The words themselves live in
 * {@code invoice-vocabulary.json}; a label that is missing is added there.
 */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(staticName = "create")
final class InvoiceVocabulary {
  private final Pattern heading;
  private final Pattern otherDocument;
  private final Pattern number;
  private final AmountLabels gross;
  private final AmountLabels net;
  private final Pattern tax;
  private final Pattern invoiceDate;
  private final Pattern dueDate;
  private final Pattern paymentTerm;
  private final Map<String, Integer> months;
}