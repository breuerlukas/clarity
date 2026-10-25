package de.lukasbreuer.clarity.api.transaction.receipt;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

/**
 * The photos and PDFs of receipts, one file per receipt named by its id, in
 * the directory of {@code [receipts] directory}. They hold personal data,
 * so they stay out of the database (and its dumps) and next to the other
 * data of the instance, where the backup picks them up.
 */
@Component
@RequiredArgsConstructor
public final class ReceiptFiles {
  private final ReceiptConfiguration configuration;

  public void store(UUID receiptId, byte[] content) throws IOException {
    Files.createDirectories(directory());
    Files.write(file(receiptId), content);
  }

  public Optional<byte[]> read(UUID receiptId) {
    try {
      return Optional.of(Files.readAllBytes(file(receiptId)));
    } catch (IOException exception) {
      return Optional.empty();
    }
  }

  public void delete(UUID receiptId) throws IOException {
    Files.deleteIfExists(file(receiptId));
  }

  private Path file(UUID receiptId) {
    return directory().resolve(receiptId.toString());
  }

  private Path directory() {
    return Path.of(configuration.directory());
  }
}