package de.lukasbreuer.clarity.api.web.receipt;

import de.lukasbreuer.clarity.api.business.invoice.reading.InvoiceFile;
import de.lukasbreuer.clarity.api.transaction.receipt.Receipt;
import de.lukasbreuer.clarity.api.transaction.receipt.RecognisedReceipt;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;

import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * A receipt file as the panel uploads it: its name, its type and the
 * content in base64. Only photos (PNG, JPEG) and PDFs up to 10 MB are
 * taken, and the type it is served as again comes from that short list,
 * never from the upload.
 */
record ReceiptUpload(InvoiceFile file) {
  private static final int MAXIMUM_SIZE = 10 * 1024 * 1024;
  private static final int MAXIMUM_NAME_LENGTH = 200;

  static Optional<ReceiptUpload> parse(ApiRequestBody body) {
    try {
      var content = Base64.getDecoder().decode(body.getString("content"));
      return Optional.of(new ReceiptUpload(new InvoiceFile(
          body.getString("name", MAXIMUM_NAME_LENGTH),
          body.getString("content_type", 100), content)))
        .filter(ReceiptUpload::isAccepted);
    } catch (IllegalArgumentException exception) {
      return Optional.empty();
    }
  }

  private boolean isAccepted() {
    return file.content().length > 0 && file.content().length <= MAXIMUM_SIZE &&
      (file.isPdf() || file.isImage());
  }

  Receipt receipt(UUID spaceId, RecognisedReceipt recognised) {
    return Receipt.create(UUID.randomUUID(), spaceId, null,
      recognised.merchant(), recognised.date(), recognised.total(),
      recognised.items(), safeName(), file.safeContentType(),
      file.content().length, System.currentTimeMillis());
  }

  /**
   * Without path separators, control characters or quotes, since the name
   * ends up in a download header.
   */
  private String safeName() {
    var name = file.name().replaceAll("[\\\\/\\p{Cntrl}\"]", "_").trim();
    return name.isEmpty() ? "receipt" : name;
  }
}