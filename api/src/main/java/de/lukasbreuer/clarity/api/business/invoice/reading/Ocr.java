package de.lukasbreuer.clarity.api.business.invoice.reading;

import de.lukasbreuer.clarity.api.log.Log;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Recognises text in images with Tesseract, which the api image installs.
 * It runs as a separate process on this machine, so no invoice leaves the
 * instance. Without Tesseract (as in a plain development setup) scans are
 * simply not read.
 */
@Component
@RequiredArgsConstructor
public final class Ocr {
  private static final long TIMEOUT_SECONDS = 60;
  private static final String[] LIST_LANGUAGES =
    {"tesseract", "--list-langs"};
  private static final String[] GERMAN_AND_ENGLISH =
    {"tesseract", "stdin", "stdout", "-l", "deu+eng"};
  private static final String[] ENGLISH =
    {"tesseract", "stdin", "stdout", "-l", "eng"};

  private final Log log;
  private volatile Optional<String[]> command;

  public boolean isAvailable() {
    return command().isPresent();
  }

  /**
   * German and English if the German data is installed (as in the api
   * image), English alone otherwise.
   */
  private Optional<String[]> command() {
    if (command == null) {
      var installed = run(LIST_LANGUAGES, null).map(String::lines)
        .map(lines -> lines.map(String::trim).toList());
      command = installed.filter(languages -> languages.contains("eng"))
        .map(languages -> languages.contains("deu") ? GERMAN_AND_ENGLISH :
          ENGLISH);
      if (command.isEmpty()) {
        log.info("Tesseract is not installed, scanned invoices are not read");
      }
    }
    return command;
  }

  /**
   * @param image A PNG or JPEG
   */
  public Optional<String> read(byte[] image) {
    if (!isAvailable()) {
      return Optional.empty();
    }
    Path input = null;
    try {
      input = Files.createTempFile("clarity-ocr", ".img");
      Files.write(input, image);
      return run(command().orElseThrow(), input);
    } catch (IOException exception) {
      return Optional.empty();
    } finally {
      deleteQuietly(input);
    }
  }

  /**
   * @param input The file Tesseract reads from its standard input, or none
   */
  private Optional<String> run(String[] command, Path input) {
    Path output = null;
    try {
      output = Files.createTempFile("clarity-ocr", ".txt");
      var builder = new ProcessBuilder(command)
        .redirectOutput(output.toFile())
        .redirectError(ProcessBuilder.Redirect.DISCARD);
      if (input != null) {
        builder.redirectInput(input.toFile());
      }
      var process = builder.start();
      if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        process.destroyForcibly();
        return Optional.empty();
      }
      return process.exitValue() == 0 ?
        Optional.of(Files.readString(output)) : Optional.empty();
    } catch (IOException exception) {
      return Optional.empty();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      return Optional.empty();
    } finally {
      deleteQuietly(output);
    }
  }

  private void deleteQuietly(Path file) {
    try {
      if (file != null) {
        Files.deleteIfExists(file);
      }
    } catch (IOException exception) {
      log.warning("Could not delete " + file);
    }
  }
}
