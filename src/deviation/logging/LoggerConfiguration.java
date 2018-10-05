package deviation.logging;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.logging.Level.*;

public class LoggerConfiguration {

  public static void configureDefaultLogger() {
    configureLogger(INFO);
  }

  public static void configureVerboseLogger() {
    configureLogger(FINE);
  }

  public static void configureExtraVerboseLogger() {
    configureLogger(FINEST);
  }

  private static void configureLogger(Level level) {
    removeAllRootLogger();
    Logger deviationLogger = Logger.getLogger("deviation");
    removeExistingHandlers(deviationLogger);
    deviationLogger.addHandler(createConsoleHandler());
    deviationLogger.setLevel(level);
  }

  private static void removeAllRootLogger() {
    Logger logger = Logger.getLogger("");
    removeExistingHandlers(logger);
  }

  private static ConsoleHandler createConsoleHandler() {
    ConsoleHandler consoleHandler = new ConsoleHandler();
    consoleHandler.setFormatter(new DeviationLogFormatter());
    consoleHandler.setLevel(ALL);
    return consoleHandler;
  }

  private static void removeExistingHandlers(Logger logger) {
    for (Handler h : logger.getHandlers()) {
      logger.removeHandler(h);
    }
  }

}
