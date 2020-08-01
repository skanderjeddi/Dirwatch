package com.skanderj.yalt;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * A class used for custom logging purposes. Features: system streams
 * redirection, (TODO) custom severity levels, process exiting when hitting a
 * fatal error (TODO make it toggleable), and much more.
 *
 * @author Skander J.
 *
 */
public final class Logger {
	// Debuggings' states
	private static boolean debug = true, developmentDebug = false, logToFile = false;

	// References to the default system streams
	private final static PrintStream DEFAULT_SYSTEM_OUTPUT = System.out;
	private final static PrintStream DEFAULT_SYSTEM_ERROR_OUTPUT = System.err;

	// Date and time format, #TODO make it customizable
	private final static SimpleDateFormat LOG_DATE_FORMAT = new SimpleDateFormat("[hh:mm:ss]");

	// Redirection currentState
	private static boolean outputRedirected = false;

	// Physical logging
	private static final File LOG_FILE = new File("logs/" + new SimpleDateFormat("MM-dd-YYYY_hh-mm-ss").format(new Date()) + ".log");
	private static BufferedWriter writer;

	private Logger() {
		return;
	}

	/**
	 * Self explanatory.
	 */
	public static void redirectSystemOutput() {
		if (Logger.outputRedirected) {
			return;
		} else {
			if (Logger.logToFile) {
				Logger.LOG_FILE.getParentFile().mkdirs();
				try {
					Logger.writer = new BufferedWriter(new FileWriter(Logger.LOG_FILE));
				} catch (final IOException ioException) {
					Logger.log(Logger.class, LogLevel.SEVERE, "Coulnd't create log file! %s", ioException.getMessage());
				}
			}
			// Display the Gingerbread version message
			System.setOut(new Logger.LoggerPrintStream(Logger.DEFAULT_SYSTEM_OUTPUT, Logger.DEFAULT_SYSTEM_ERROR_OUTPUT));
			System.setErr(new Logger.LoggerPrintStream(Logger.DEFAULT_SYSTEM_ERROR_OUTPUT, Logger.DEFAULT_SYSTEM_ERROR_OUTPUT));
			Logger.outputRedirected = true;
			Logger.log(Logger.class, LogLevel.DEBUG, "Successfully redirected default output streams");
			Logger.debug = false;
			Logger.developmentDebug = false;
			Logger.log(Logger.class, LogLevel.INFO, "Disabled all debug messages");
			if (Logger.logToFile) {
				Logger.log(Logger.class, LogLevel.INFO, "Logging to %s!", Logger.LOG_FILE.getPath());
			}
		}
	}

	public static void cleanUp() {
		if (Logger.logToFile) {
			try {
				Logger.writer.flush();
				Logger.writer.close();
			} catch (final IOException ioException) {
				Logger.log(Logger.class, LogLevel.FATAL, "Could not write log to log file! (%s)", ioException.getMessage());
			}
		}
	}

	/**
	 * Self explanatory. A FATAL log level will exit all processes. The "message"
	 * string will be formatted with the "args" parameter.
	 */
	public static void log(final Class<?> clazz, final LogLevel logLevel, final String message, final Object... args) {
		String origin = new String();
		if (clazz.getEnclosingClass() != null) {
			origin = clazz.getEnclosingClass().getSimpleName() + "#" + clazz.getSimpleName();
		} else {
			origin = clazz.getSimpleName();
		}
		String finalMessage = String.format(Logger.LOG_DATE_FORMAT.format(new Date()) + " [" + origin + " / " + logLevel.name() + "]: " + message + "\n", args);
		if ((logLevel == LogLevel.SEVERE) || (logLevel == LogLevel.ERROR) || (logLevel == LogLevel.FATAL)) {
			Logger.DEFAULT_SYSTEM_ERROR_OUTPUT.printf(finalMessage);
		} else {
			if (logLevel == LogLevel.DEBUG) {
				if (Logger.debug) {
					Logger.DEFAULT_SYSTEM_OUTPUT.printf(finalMessage);
				}
			} else if (logLevel == LogLevel.DEVELOPMENT) {
				if (Logger.developmentDebug) {
					Logger.DEFAULT_SYSTEM_OUTPUT.printf(finalMessage);
				}
			} else {
				Logger.DEFAULT_SYSTEM_OUTPUT.printf(finalMessage);
			}
		}
		if (logLevel == LogLevel.FATAL) {
			finalMessage = String.format(Logger.LOG_DATE_FORMAT.format(new Date()) + " [" + Logger.class.getSimpleName() + " / " + LogLevel.FATAL.name() + "]: A fatal log has been submitted from %s.class, exiting all processes \n", clazz.getSimpleName());
			Logger.DEFAULT_SYSTEM_ERROR_OUTPUT.printf(finalMessage);
			Logger.cleanUp();
			System.exit(-1);
		}
		if (Logger.logToFile) {
			try {
				Logger.writer.append(finalMessage);
			} catch (final IOException ioException) {
				Logger.log(Logger.class, LogLevel.SEVERE, "Could not write log to log file! (%s)", ioException.getMessage());
			}
		}
	}

	/**
	 * Self explanatory.
	 */
	public static void setStateForDebuggingType(final DebuggingType type, final boolean status) {
		Logger.redirectSystemOutput();
		switch (type) {
		case CLASSIC:
			Logger.debug = status;
			Logger.log(Logger.class, LogLevel.INFO, "Enabled regular debugging messages");
			break;
		case DEVELOPMENT:
			Logger.log(Logger.class, LogLevel.INFO, "Enabled development debugging messages");
			Logger.developmentDebug = status;
			break;
		}
	}

	public static void toggleLoggingToFile() {
		Logger.logToFile = !Logger.logToFile;
		if (Logger.logToFile && (Logger.writer == null)) {
			Logger.LOG_FILE.getParentFile().mkdirs();
			try {
				Logger.writer = new BufferedWriter(new FileWriter(Logger.LOG_FILE));
			} catch (final IOException ioException) {
				Logger.log(Logger.class, LogLevel.SEVERE, "Coulnd't create log file! %s", ioException.getMessage());
			}
			Logger.log(Logger.class, LogLevel.INFO, "Logging to %s!", Logger.LOG_FILE.getPath());
		}
	}

	/**
	 * Custom print streams for when redirection hasn't happened yet.
	 *
	 * @author Skander
	 *
	 */
	private static class LoggerPrintStream extends PrintStream {
		private final PrintStream printStream;

		public LoggerPrintStream(final OutputStream out, final PrintStream printStream) {
			super(out);
			this.printStream = printStream;
		}

		/**
		 * Self explanatory.
		 */
		@Override
		public PrintStream printf(final String format, final Object... args) {
			final String finalMessage = String.format(Logger.LOG_DATE_FORMAT.format(new Date()) + " [? / ?]: " + format, args);
			if (Logger.logToFile) {
				try {
					Logger.writer.append(finalMessage);
				} catch (final IOException ioException) {
					Logger.log(Logger.class, LogLevel.SEVERE, "Could not write log to log file! (%s)", ioException.getMessage());
				}
			}
			return this.printStream.printf(finalMessage);
		}

		/**
		 * Self explanatory.
		 */
		@Override
		public PrintStream printf(final Locale l, final String format, final Object... args) {
			final String finalMessage = String.format(Logger.LOG_DATE_FORMAT.format(new Date()) + " [? / ?]: " + format, args);
			if (Logger.logToFile) {
				try {
					Logger.writer.append(finalMessage);
				} catch (final IOException ioException) {
					Logger.log(Logger.class, LogLevel.SEVERE, "Could not write log to log file! (%s)", ioException.getMessage());
				}
			}
			return this.printStream.printf(l, finalMessage);
		}

		/**
		 * Self explanatory.
		 */
		@Override
		public void print(final Object obj) {
			final String finalMessage = String.format(Logger.LOG_DATE_FORMAT.format(new Date()) + " [? / ?]: " + obj);
			if (Logger.logToFile) {
				try {
					Logger.writer.append(finalMessage);
				} catch (final IOException ioException) {
					Logger.log(Logger.class, LogLevel.SEVERE, "Could not write log to log file! (%s)", ioException.getMessage());
				}
			}
			this.printStream.print(finalMessage);
		}

		/**
		 * Self explanatory.
		 */
		@Override
		public void print(final boolean b) {
			this.print((Object) b);
		}

		/**
		 * Self explanatory.
		 */
		@Override
		public void print(final char c) {
			this.print((Object) c);
		}

		/**
		 * Self explanatory.
		 */
		@Override
		public void print(final char[] s) {
			this.print((Object) s);
		}

		/**
		 * Self explanatory.
		 */
		@Override
		public void print(final double d) {
			this.print((Object) d);
		}

		/**
		 * Self explanatory.
		 */
		@Override
		public void print(final float f) {
			this.print((Object) f);
		}

		/**
		 * Self explanatory.
		 */
		@Override
		public void print(final int i) {
			this.print((Object) i);
		}

		/**
		 * Self explanatory.
		 */
		@Override
		public void print(final long l) {
			this.print((Object) l);
		}

		/**
		 * Self explanatory.
		 */
		@Override
		public void print(final String s) {
			this.print((Object) s);
		}

		/**
		 * Self explanatory.
		 */
		@Override
		public void println(final Object x) {
			final String finalMessage = String.format(Logger.LOG_DATE_FORMAT.format(new Date()) + " [? / ?]: " + x);
			if (Logger.logToFile) {
				try {
					Logger.writer.append(finalMessage);
				} catch (final IOException ioException) {
					Logger.log(Logger.class, LogLevel.SEVERE, "Could not write log to log file! (%s)", ioException.getMessage());
				}
			}
			this.printStream.println(finalMessage);

		}

		/**
		 * Self explanatory.
		 */
		@Override
		public void println() {
			this.printStream.println();
		}

		/**
		 * Self explanatory.
		 */
		@Override
		public void println(final boolean x) {
			this.println((Object) x);
		}

		/**
		 * Self explanatory.
		 */
		@Override
		public void println(final char x) {
			this.println((Object) x);
		}

		/**
		 * Self explanatory.
		 */
		@Override
		public void println(final char[] x) {
			this.println((Object) x);
		}

		/**
		 * Self explanatory.
		 */
		@Override
		public void println(final double x) {
			this.println((Object) x);
		}

		/**
		 * Self explanatory.
		 */
		@Override
		public void println(final float x) {
			this.println((Object) x);
		}

		/**
		 * Self explanatory.
		 */
		@Override
		public void println(final int x) {
			this.println((Object) x);
		}

		/**
		 * Self explanatory.
		 */
		@Override
		public void println(final long x) {
			this.println((Object) x);
		}

		/**
		 * Self explanatory.
		 */
		@Override
		public void println(final String x) {
			this.println((Object) x);
		}
	}
}
