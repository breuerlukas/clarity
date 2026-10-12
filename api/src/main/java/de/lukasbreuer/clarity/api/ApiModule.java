package de.lukasbreuer.clarity.api;

import de.lukasbreuer.clarity.api.web.WebModule;
import de.lukasbreuer.clarity.api.log.Log;
import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.io.File;

@Configuration
@Import(WebModule.class)
public class ApiModule {
  @Bean
  Log apiLog() throws Exception {
    return Log.create("Api");
  }

  @Bean
  INIConfiguration configurationFile() throws Exception {
    return new Configurations().ini(new File("config.ini"));
  }
}