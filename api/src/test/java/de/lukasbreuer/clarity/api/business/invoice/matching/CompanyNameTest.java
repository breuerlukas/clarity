package de.lukasbreuer.clarity.api.business.invoice.matching;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CompanyNameTest {
  @Test
  void comparesCompanyNames() {
    assertTrue(CompanyName.of("Müller Logistik GmbH")
      .isSimilarTo("MUELLER LOGISTIK"));
    assertTrue(CompanyName.of("Nordlicht Hosting GmbH")
      .isSimilarTo("NORDLICHT"));
    assertFalse(CompanyName.of("Kunde GmbH & Co. KG")
      .isSimilarTo("Andere GmbH"));
  }
}