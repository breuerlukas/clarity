package de.lukasbreuer.clarity.api.transaction.receipt;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.json.JSONArray;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Stores the items of a receipt as a JSON array in a single column; they
 * are always read and written together.
 */
@Converter
public final class ReceiptItemListConverter
  implements AttributeConverter<List<ReceiptItem>, String> {
  @Override
  public String convertToDatabaseColumn(List<ReceiptItem> items) {
    return new JSONArray(items.stream().map(ReceiptItem::information)
      .toList()).toString();
  }

  @Override
  public List<ReceiptItem> convertToEntityAttribute(String column) {
    var array = new JSONArray(column);
    return IntStream.range(0, array.length()).mapToObj(array::getJSONObject)
      .map(ReceiptItem::of).toList();
  }
}