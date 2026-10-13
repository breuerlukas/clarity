package de.lukasbreuer.clarity.api.business.invoice.reading;

import de.lukasbreuer.clarity.api.log.Log;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Recognises text in images with Tesseract, which the api image installs.
 * It runs as a separate process on this machine, so no invoice leaves the
 * instance. Without Tesseract (as in a plain development setup) scans are
 * simply not read.
 */
@Component
@RequiredArgsConstructor
public final class Ocr {
  private static final List<String> LANGUAGES = List.of("deu", "eng");
  private static final long TIMEOUT_SECONDS = 60;

  private final Log log;
  private volatile Optional<String> languages;

  public boolean isAvailable() {
    return languages().isPresent();
  }

  /**
   * German and English as far as they are installed, joined the way
   * Tesseract takes them ("deu+eng").
   */
  private Optional<String> languages() {
    if (languages == null) {
      languages = run("--list-langs").map(installed -> LANGUAGES.stream()
        .filter(language -> installed.lines().map(String::trim)
          .anyMatch(language::equals))
        .collect(Collectors.joining("+")))
        .filter(joined -> !joined.isEmpty());
      if (languages.isEmpty()) {
        log.info("Tesseract is not installed, scanned invoices are not read");
      }
    }
    return languages;
  }

  /**
   * @param image A PNG or JPEG
   */
  public Optional<String> read(byte[] image) {
    if (!isAvailable()) {
      return Optional.empty();
    }
    var language = languages().orElseThrow();
    Path input = null;
    try {
      input = Files.createTempFile("clarity-ocr", ".img");
      Files.write(input, image);
      return run(input.toString(), "stdout", "-l", language);
    } catch (IOException exception) {
      return Optional.empty();
    } finally {
      deleteQuietly(input);
    }
  }

  private Optional<String> run(String... arguments) {
    Path output = null;
    try {
      output = Files.createTempFile("clarity-ocr", ".txt");
      var command = new String[arguments.length + 1];
      command[0] = "tesseract";
      System.arraycopy(arguments, 0, command, 1, arguments.length);
      var process = new ProcessBuilder(command)
        .redirectOutput(output.toFile())
        .redirectError(ProcessBuilder.Redirect.DISCARD)
        .start();
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
