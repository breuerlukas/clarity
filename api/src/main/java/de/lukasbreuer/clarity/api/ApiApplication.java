package de.lukasbreuer.clarity.api;

import de.lukasbreuer.clarity.api.web.WebConfiguration;
import de.lukasbreuer.clarity.api.log.Log;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Collections;

@EnableScheduling
@SpringBootApplication(scanBasePackages = {"de.lukasbreuer.clarity.api"})
public class ApiApplication {
  /**
   * The starting point where the application is executed
   *
   * @param args The arguments that are passed into the application
   */
  public static void main(String[] args) {
    try (var apiContext =
           new AnnotationConfigApplicationContext(ApiModule.class)) {
      var log = apiContext.getBean(Log.class);
      Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
        log.processError(throwable));
      try {
        log.info("Initializing Clarity - Api");
        var application = new SpringApplication(ApiApplication.class);
        var webConfiguration = apiContext.getBean(WebConfiguration.class);
        application.setDefaultProperties(Collections.singletonMap("server.port",
          webConfiguration.port()));
        log.info("Booting Spring...");
        application.run(args);
        log.info("Successfully booted Clarity - Api");
      } catch (Exception exception) {
        log.processError(exception);
      }
    }
  }
}