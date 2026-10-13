package de.lukasbreuer.clarity.api.secret;

import de.lukasbreuer.clarity.api.configuration.Configuration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.commons.configuration2.INIConfiguration;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(staticName = "create")
public final class SecretConfiguration implements Configuration {
  private String keyFile;

  @Override
  public void load(INIConfiguration file) {
    keyFile = file.getString("secrets.key_file", "data/clarity.key");
  }
}
