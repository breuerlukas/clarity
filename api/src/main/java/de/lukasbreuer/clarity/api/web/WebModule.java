package de.lukasbreuer.clarity.api.web;

import org.apache.commons.configuration2.INIConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebModule {
  @Bean
  WebConfiguration webConfiguration(INIConfiguration file) {
    var configuration = WebConfiguration.create();
    configuration.load(file);
    return configuration;
  }
}