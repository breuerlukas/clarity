package de.lukasbreuer.clarity.api.transaction.receipt;

import de.lukasbreuer.clarity.api.configuration.Configuration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.commons.configuration2.INIConfiguration;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(staticName = "create")
public final class ReceiptConfiguration implements Configuration {
  private String directory;

  @Override
  public void load(INIConfiguration file) {
    directory = file.getString("receipts.directory", "data/receipts");
  }
}