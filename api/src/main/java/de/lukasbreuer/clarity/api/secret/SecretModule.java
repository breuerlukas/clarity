package de.lukasbreuer.clarity.api.secret;

import org.apache.commons.configuration2.INIConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration
public class SecretModule {
  @Bean
  SecretConfiguration secretConfiguration(INIConfiguration file) {
    var configuration = SecretConfiguration.create();
    configuration.load(file);
    return configuration;
  }

  @Bean
  SecretBox secretBox(SecretConfiguration configuration) throws Exception {
    return SecretBox.load(Path.of(configuration.keyFile()));
  }
}