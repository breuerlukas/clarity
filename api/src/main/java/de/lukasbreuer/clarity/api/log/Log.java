package de.lukasbreuer.clarity.api.log;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.*;

@Accessors(fluent = true)
public final class Log extends Logger {
  public static Log create(String name) throws Exception {
    var consoleHandler = new ConsoleHandler();
    consoleHandler.setFormatter(LogFormat.create(LogFormat.FormatType.CONSOLE));
    consoleHandler.setLevel(Level.ALL);
    var log = new Log(name, consoleHandler);
    log.setLevel(Level.ALL);
    log.addHandler(consoleHandler);
    Runtime.getRuntime().addShutdownHook(new Thread(log::close));
    return log;
  }

  @Getter
  private final String name;
  private final ConsoleHandler consoleHandler;

  private Log(
    String name, ConsoleHandler consoleHandler
  ) {
    super(name, null);
    this.name = name;
    this.consoleHandler = consoleHandler;
  }

  @Override
  public void log(LogRecord record) {
    super.log(formatRecord(record));
  }

  public void consoleLog(Level level, String message) {
    restrictedLog(consoleHandler, new LogRecord(level, message));
  }

  private void restrictedLog(Handler handler, LogRecord record) {
    handler.publish(formatRecord(record));
  }

  private LogRecord formatRecord(LogRecord record) {
    record.setMessage("[" + getName().toUpperCase() + "] " + record.getMessage());
    return record;
  }

  public void processError(Throwable throwable) {
    var stringWriter = new StringWriter();
    var printWriter = new PrintWriter(stringWriter);
    throwable.printStackTrace(printWriter);
    var lines = stringWriter.toString().split("\n");
    for (var line : lines) {
      warning(line.toString());
    }
  }

  public void close() {
    for (var handler : getHandlers()) {
      handler.close();
    }
  }
}
