package de.lukasbreuer.clarity.api.business.invoice.reading;

import lombok.RequiredArgsConstructor;

import java.io.IOException;

/**
 * A sample invoice file from the test resources under {@code /invoices}.
 */
@RequiredArgsConstructor(staticName = "of")
final class InvoiceResource {
  private final String name;

  byte[] bytes() throws IOException {
    try (var stream = InvoiceResource.class.getResourceAsStream(
      "/invoices/" + name)) {
      return stream.readAllBytes();
    }
  }
}