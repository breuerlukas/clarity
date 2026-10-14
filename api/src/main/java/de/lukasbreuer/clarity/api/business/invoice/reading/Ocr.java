package de.lukasbreuer.clarity.api.business.invoice.reading;

import de.lukasbreuer.clarity.api.log.Log;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Recognises text in images with Tesseract, which the api image installs.
 * It runs as a separate process on this machine, so no invoice leaves the
 * instance. Without Tesseract (as in a plain development setup) scans are
 * simply not read.
 */
@Component
@RequiredArgsConstructor(staticName = "create")
public final class Ocr {
  private static final String[] LIST_LANGUAGES =
    {"tesseract", "--list-langs"};
  private static final String[] GERMAN_AND_ENGLISH =
    {"tesseract", "stdin", "stdout", "-l", "deu+eng"};
  private static final String[] ENGLISH =
    {"tesseract", "stdin", "stdout", "-l", "eng"};

  private final Log log;
  private volatile Optional<String[]> detectedCommand;

  public boolean isAvailable() {
    return command().isPresent();
  }

  /**
   * @param image A PNG or JPEG
   */
  public Optional<String> read(byte[] image) {
    return command().flatMap(tesseract -> TesseractRun.of(tesseract, log)
      .outputFor(image));
  }

  /**
   * Looked up once, on the first scan.
   */
  private Optional<String[]> command() {
    if (detectedCommand == null) {
      detectedCommand = detectCommand();
    }
    return detectedCommand;
  }

  /**
   * German and English if the German data is installed (as in the api
   * image), English alone otherwise.
   */
  private Optional<String[]> detectCommand() {
    var command = installedLanguages()
      .filter(languages -> languages.contains("eng"))
      .map(languages -> languages.contains("deu") ? GERMAN_AND_ENGLISH :
        ENGLISH);
    if (command.isEmpty()) {
      log.info("Tesseract is not installed, scanned invoices are not read");
    }
    return command;
  }

  private Optional<List<String>> installedLanguages() {
    return TesseractRun.of(LIST_LANGUAGES, log).output()
      .map(output -> output.lines().map(String::trim).toList());
  }
}