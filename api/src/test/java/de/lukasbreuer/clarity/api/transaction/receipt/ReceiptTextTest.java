package de.lukasbreuer.clarity.api.transaction.receipt;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class ReceiptTextTest {
  private static final ReceiptVocabulary VOCABULARY =
    ReceiptVocabularyFile.load().vocabulary();
  private static final String SUPERMARKET = """
    REWE Markt GmbH
    Musterstraße 1, 80331 München
    UID Nr. DE812706034
    EUR
    BANANEN                1,99 B
    VOLLMILCH 3,5%         1,19 B
    2 Stk x 0,99
    BRÖTCHEN               1,98 B
    ZAHNPASTA              2,45 A
    PFAND                 -0,25 A
    --------------------------------
    SUMME           EUR    7,36
    Geg. Girocard   EUR    7,36
    Steuer %    Netto   Steuer  Brutto
    A= 19,0%     1,85     0,35    2,20
    B=  7,0%     4,82     0,34    5,16
    19.10.26     12:04     Bon-Nr.: 4711
    """;

  @Test
  void readsMerchantDateAndTotal() {
    var receipt = ReceiptText.of(VOCABULARY, SUPERMARKET).recognise();
    assertEquals("REWE Markt GmbH", receipt.merchant());
    assertEquals(LocalDate.of(2026, 10, 19), receipt.date());
    assertEquals(new BigDecimal("7.36"), receipt.total());
  }

  @Test
  void readsTheItemsAboveTheTotal() {
    var items = ReceiptText.of(VOCABULARY, SUPERMARKET).items();
    assertEquals(List.of(
      new ReceiptItem("BANANEN", new BigDecimal("1.99")),
      new ReceiptItem("VOLLMILCH 3,5%", new BigDecimal("1.19")),
      new ReceiptItem("BRÖTCHEN", new BigDecimal("1.98")),
      new ReceiptItem("ZAHNPASTA", new BigDecimal("2.45")),
      new ReceiptItem("PFAND", new BigDecimal("-0.25"))), items);
  }

  @Test
  void readsEnglishReceipts() {
    var receipt = ReceiptText.of(VOCABULARY, """
      Corner Café
      Flat white      3.80
      Croissant       2.60
      Total           6.40
      Card            6.40
      2026-10-18
      """).recognise();
    assertEquals("Corner Café", receipt.merchant());
    assertEquals(new BigDecimal("6.40"), receipt.total());
    assertEquals(2, receipt.items().size());
  }

  @Test
  void leavesWhatIsMissingEmpty() {
    var receipt = ReceiptText.of(VOCABULARY, "~~ ## ~~").recognise();
    assertEquals("", receipt.merchant());
    assertNull(receipt.date());
    assertNull(receipt.total());
  }
}