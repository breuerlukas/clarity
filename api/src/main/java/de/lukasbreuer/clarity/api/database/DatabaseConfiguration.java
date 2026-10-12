package de.lukasbreuer.clarity.api.database;

import de.lukasbreuer.clarity.api.configuration.Configuration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.commons.configuration2.INIConfiguration;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(staticName = "create")
public final class DatabaseConfiguration implements Configuration {
  private String hostname;
  private int port;
  private String username;
  private String password;
  private String database;

  @Override
  public void load(INIConfiguration file) {
    hostname = file.getString("database.hostname");
    port = file.getInt("database.port");
    username = file.getString("database.username");
    password = file.getString("database.password");
    database = file.getString("database.database");
  }
}
