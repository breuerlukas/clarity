package de.lukasbreuer.clarity.api.banking;

import org.apache.commons.configuration2.INIConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class BankingModule {
  @Bean
  BankingConfiguration bankingConfiguration(INIConfiguration file) {
    var configuration = BankingConfiguration.create();
    configuration.load(file);
    return configuration;
  }

  @Bean
  HttpClient bankingHttpClient() {
    return HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(20))
      .followRedirects(HttpClient.Redirect.NEVER)
      .build();
  }
}