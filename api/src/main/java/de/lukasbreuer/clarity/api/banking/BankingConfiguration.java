package de.lukasbreuer.clarity.api.banking;

import de.lukasbreuer.clarity.api.configuration.Configuration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.commons.configuration2.INIConfiguration;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(staticName = "create")
public final class BankingConfiguration implements Configuration {
  private String applicationId;
  private String keyFile;
  private String redirectUrl;

  @Override
  public void load(INIConfiguration file) {
    applicationId = file.getString("banking.application_id", "");
    keyFile = file.getString("banking.key_file");
    redirectUrl = file.getString("banking.redirect_url");
  }
}