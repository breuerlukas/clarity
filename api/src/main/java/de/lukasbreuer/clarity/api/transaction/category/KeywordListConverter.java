package de.lukasbreuer.clarity.api.transaction.category;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

/**
 * Stores the keywords of a category as one line each in a single column;
 * they are always read and written together, and a keyword never contains
 * a line break.
 */
@Converter
public final class KeywordListConverter
  implements AttributeConverter<List<String>, String> {
  private static final String SEPARATOR = "\n";

  @Override
  public String convertToDatabaseColumn(List<String> keywords) {
    return String.join(SEPARATOR, keywords);
  }

  @Override
  public List<String> convertToEntityAttribute(String column) {
    return column.isEmpty() ? List.of() : List.of(column.split(SEPARATOR));
  }
}