package de.lukasbreuer.clarity.api.business.invoice.reading;

import java.util.Locale;

/**
 * A file that may be an invoice: an attachment of a mail.
 */
public record InvoiceFile(String name, String contentType, byte[] content) {
  public boolean isXml() {
    return hasType("xml") || hasExtension(".xml");
  }

  public boolean isPdf() {
    return hasType("pdf") || hasExtension(".pdf");
  }

  public boolean isImage() {
    return hasType("image/png") || hasType("image/jpeg") ||
      hasExtension(".png") || hasExtension(".jpg") || hasExtension(".jpeg");
  }

  /**
   * The type the file is served as again, from the short list of types the
   * import keeps, never what the sender claimed.
   */
  public String safeContentType() {
    if (isPdf()) {
      return "application/pdf";
    }
    if (isXml()) {
      return "application/xml";
    }
    if (hasType("image/png") || hasExtension(".png")) {
      return "image/png";
    }
    return "image/jpeg";
  }

  private boolean hasType(String type) {
    return contentType != null &&
      contentType.toLowerCase(Locale.ROOT).contains(type);
  }

  private boolean hasExtension(String extension) {
    return name != null && name.toLowerCase(Locale.ROOT).endsWith(extension);
  }
}
