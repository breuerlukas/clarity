package de.lukasbreuer.clarity.api.business.invoice.reading;

import java.util.Optional;

/**
 * What a PDF holds.
 *
 * @param xml The embedded e-invoice, if there is one
 * @param recognised Whether the text came from OCR
 */
public record PdfContent(
  Optional<byte[]> xml, String text, boolean recognised
) {
}