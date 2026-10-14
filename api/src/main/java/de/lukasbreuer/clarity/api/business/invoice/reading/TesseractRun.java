package de.lukasbreuer.clarity.api.business.invoice.reading;

import de.lukasbreuer.clarity.api.log.Log;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * One run of Tesseract with one of the fixed commands of {@link Ocr}; the
 * image goes in on standard input, so no file name ever reaches the
 * command line. A run that fails or takes too long yields nothing.
 */
@RequiredArgsConstructor(staticName = "of")
final class TesseractRun {
  private static final long TIMEOUT_SECONDS = 60;

  private final String[] command;
  private final Log log;

  Optional<String> output() {
    return run(null);
  }

  /**
   * @param image A PNG or JPEG
   */
  Optional<String> outputFor(byte[] image) {
    Path input = null;
    try {
      input = Files.createTempFile("clarity-ocr", ".img");
      Files.write(input, image);
      return run(input);
    } catch (IOException exception) {
      return Optional.empty();
    } finally {
      deleteQuietly(input);
    }
  }

  /**
   * @param input The file Tesseract reads from its standard input, or none
   */
  private Optional<String> run(Path input) {
    Path output = null;
    try {
      output = Files.createTempFile("clarity-ocr", ".txt");
      var process = processBuilder(input, output).start();
      return succeeded(process) ? Optional.of(Files.readString(output)) :
        Optional.empty();
    } catch (IOException exception) {
      return Optional.empty();
    } finally {
      deleteQuietly(output);
    }
  }

  private ProcessBuilder processBuilder(Path input, Path output) {
    var builder = new ProcessBuilder(command)
      .redirectOutput(output.toFile())
      .redirectError(ProcessBuilder.Redirect.DISCARD);
    return input == null ? builder : builder.redirectInput(input.toFile());
  }

  private boolean succeeded(Process process) {
    try {
      var finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
      if (!finished) {
        process.destroyForcibly();
      }
      return finished && process.exitValue() == 0;
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      return false;
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