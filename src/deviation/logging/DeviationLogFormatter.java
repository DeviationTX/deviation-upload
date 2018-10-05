package deviation.logging;

import org.apache.commons.lang3.StringUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.LogRecord;

/**
 * Overrides {@link java.util.logging.SimpleFormatter}
 * in order to use international string formatting
 * and using a single line default formatting string
 */
class DeviationLogFormatter extends java.util.logging.SimpleFormatter {

  private static final String LOG_FORMAT = "[%1$tFT%1$tT.%1$tL%1$tz] (%4$s) %3$s: %5$s%6$s%n";

  public synchronized String format(LogRecord record) {
    String source;
    if (record.getSourceClassName() != null) {
      source = record.getSourceClassName();
      if (record.getSourceMethodName() != null) {
        source += " " + record.getSourceMethodName();
      }
    } else {
      source = record.getLoggerName();
    }
    String throwable = "";
    if (record.getThrown() != null) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      pw.println();
      record.getThrown().printStackTrace(pw);
      pw.close();
      throwable = sw.toString();
    }
    return String.format(LOG_FORMAT,
        new Date(record.getMillis()),
        source,
        makeSimplyfiedName(record),
        record.getLevel().getName(),
        formatMessage(record),
        throwable);
  }

  private String makeSimplyfiedName(LogRecord record) {
    String name = record.getLoggerName();
    if (name != null) {
      String[] parts = name.split("[.]");
      for (int i = 0; i < parts.length; i++) {
        if (i < parts.length - 1) {
          parts[i] = (parts[i].length() > 1) ? parts[i].substring(0, 1) : parts[i];
        }
      }
      name = StringUtils.join(parts, '.');
    }
    return name;
  }
}
