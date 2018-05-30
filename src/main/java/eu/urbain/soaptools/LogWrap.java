package eu.urbain.soaptools;

import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

//singleton class logger wrapper
public class LogWrap {
	public static final Logger aLogger = Logger.getLogger("myLogger");
	private static LogWrap instance = null;

	public static LogWrap getInstance() {
		if (instance == null) {
			getLoggerReady();
			instance = new LogWrap();
		}
		return instance;
	}

	private static void getLoggerReady() {
		try {
			FileHandler fh = new FileHandler("logs/errors.log");
			fh.setFormatter(new SimpleFormatter());
			aLogger.addHandler(fh);
			aLogger.setUseParentHandlers(false);
			aLogger.setLevel(Level.ALL);
		} catch (Exception e) {
			System.out.print("Error: Logger creation issue: " + e);
		}
	}
}
