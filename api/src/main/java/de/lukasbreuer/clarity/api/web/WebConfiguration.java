package de.lukasbreuer.clarity.api.web;

import de.lukasbreuer.clarity.api.configuration.Configuration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.commons.configuration2.INIConfiguration;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(staticName = "create")
public final class WebConfiguration implements Configuration {
  private int port;
  private boolean secureCookies;

  @Override
  public void load(INIConfiguration file) {
    port = file.getInt("web.port");
    secureCookies = file.getBoolean("web.secure_cookies", true);
  }
}