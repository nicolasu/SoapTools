package eu.urbain.soaptools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

public class SoapThread implements Runnable {

	private final String threadName;
	private final int threadIndex;
	private final String serviceName;
	private final String serviceUrl;
	private final String serviceRequestXmlPath;
	private final int serviceWait;
	private final int serviceRepetition;

	private Logger logger;
	private FileAppender appender;

	private LogWrap logWrap;
	private FileWriter fw;

	String fileSeparator = System.getProperty("file.separator");

	SoapThread(int threadIndex_, String threadName_, String serviceName_, String serviceUrl_, String serviceRequestXmlPath_, int serviceWait_, int serviceRepetition_)
			throws UnsupportedEncodingException {
		serviceName = serviceName_;
		serviceUrl = serviceUrl_;
		serviceRequestXmlPath = serviceRequestXmlPath_;
		serviceWait = serviceWait_;
		serviceRepetition = serviceRepetition_;
		threadName = threadName_;
		threadIndex = threadIndex_;

		// the log file by thread
		appender = new FileAppender();
		appender.setFile("logs" + fileSeparator + threadName + ".log");
		appender.setLayout(new PatternLayout("%d [%t] %-5p %c - %m%n"));
		appender.activateOptions();
		logger = Logger.getLogger(threadName);
		logger.setAdditivity(false);
		logger.addAppender(appender);
		logger.info("[Constructor] - Welcome in the log of thread " + threadName);

		// the log file for errors (used by all the threads)
		logWrap = LogWrap.getInstance();

		// create CSV file
		String pathJar = getProgramPath();
		String pathCsv = pathJar + fileSeparator + "csv";
		new File(pathCsv).mkdirs();
		String fullCSVPath = pathCsv + fileSeparator + threadName + ".csv";
		File file = new File(fullCSVPath);
		if (file.exists()) {
			try {
				fw = new FileWriter(file, true);
			} catch (IOException e) {
				logErrorInBothLog("CSV file " + fullCSVPath + " already exist and can't continue it", e);
			}
		} else {
			try {
				file.createNewFile();
				fw = new FileWriter(file);
			} catch (IOException e) {
				logErrorInBothLog("Can't create new CSV file or get the FileWriter " + fullCSVPath, e);
			}
		}
	}

	@Override
	public void run() {
		logger.info(
				"Start SoapThread with parameter [" + threadName + "] [" + serviceName + "] [" + serviceUrl + "] [" + serviceRequestXmlPath + "] [" + serviceWait + "] [" + serviceRepetition + "]");
		String requestString = readFile(serviceRequestXmlPath);
		logger.info(requestString);
		URL u;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		int httpCode = 0;
		for (int iLoop = 1; iLoop <= serviceRepetition; iLoop++) {
			Date startTime = new Date();

			try {
				u = new URL(serviceUrl);
				URLConnection uc = u.openConnection();
				HttpURLConnection connection = (HttpURLConnection) uc;
				connection.setDoOutput(true);
				connection.setDoInput(true);
				connection.setRequestMethod("POST");
				connection.setRequestProperty("Content-Type", "application/soap+xml; charset=utf-8");
				connection.setRequestProperty("Expect", "100-continue");
				// connection.setRequestProperty("SOAPAction", SOAP_ACTION);
				OutputStream out = connection.getOutputStream();
				Writer wout = new OutputStreamWriter(out);
				wout.write(requestString);
				wout.flush();
				wout.close();
				httpCode = connection.getResponseCode();
				logger.info("Send request " + iLoop + "/" + serviceRepetition);
				InputStream in = connection.getInputStream();
				Date endTime = new Date();
				long elapsedTime = getElapsedTime(startTime, endTime);
				logger.info("Response " + iLoop + "/" + serviceRepetition + " - time: " + elapsedTime + "ms");
				String text = IOUtils.toString(in, StandardCharsets.UTF_8.name());
				logger.info(text);
				writeInCSV(threadIndex, serviceName, startTime, endTime, elapsedTime, httpCode);
				in.close();
				connection.disconnect();
				System.out.println("Response for thread " + threadName + " HTTP Code: " + httpCode + " - response time : " + elapsedTime);
			} catch (MalformedURLException e) {
				logErrorInBothLog("Can't call the webservice, MalformedURLException", e);
				Date endTime = new Date();
				long elapsedTime = getElapsedTime(startTime, endTime);
				writeInCSV(threadIndex, serviceName, startTime, endTime, elapsedTime, httpCode);
				System.out.println("Response for thread " + threadName + " HTTP Code: " + httpCode + " - response time : " + elapsedTime);
			} catch (IOException e) {
				logErrorInBothLog("Can't call the webservice, IOException", e);
				Date endTime = new Date();
				long elapsedTime = getElapsedTime(startTime, endTime);
				writeInCSV(threadIndex, serviceName, startTime, endTime, elapsedTime, httpCode);
				System.out.println("Response for thread " + threadName + " HTTP Code: " + httpCode + " - response time : " + elapsedTime);
			}
			try {
				Thread.sleep(serviceWait);
			} catch (InterruptedException e) {
				logErrorInBothLog("Can't sleep the thread", e);
			}
		}

		// Remove appender
		logger.removeAppender(appender);

		try {
			fw.flush();
			fw.close();
		} catch (IOException e) {
			logErrorInBothLog("Can't close the FileWriter", e);
		}

	}

	private String readFile(String path) {
		String result = "";
		InputStream is = null;
		// first, try to read the request file in the same directory as the jar file
		try {
			logger.info("Try read file " + serviceRequestXmlPath + " in same directory than the jar");
			is = new FileInputStream("./" + path);
		} catch (Exception e) {
			logErrorInBothLog("Can't read request file in the jar directory", e);

			// if can't read request file in the jar directory, will search the request file
			// inside the jar
			logger.info("Try read file " + serviceRequestXmlPath + " inside the jar");
			ClassLoader classLoader = getClass().getClassLoader();
			try {
				is = classLoader.getResourceAsStream(serviceRequestXmlPath);
			} catch (Exception ex) {
				logErrorInBothLog("Can't read request file in the jar", e);
			}
		}
		if (is != null) {
			try {
				result = IOUtils.toString(is, "UTF-8");
				is.close();
			} catch (IOException e) {
				logErrorInBothLog("Find the request file but can't open it", e);
			}
		}
		return result;
	}

	public String getProgramPath() {
		try {
			URL url = SoapThread.class.getProtectionDomain().getCodeSource().getLocation();
			String jarPath = URLDecoder.decode(url.getFile(), "UTF-8");
			String parentPath = new File(jarPath).getParentFile().getPath();
			return parentPath;
		} catch (Exception e) {
			logErrorInBothLog("Can't get the path of the Jar file", e);
			return "";
		}
	}

	public long getElapsedTime(Date startDate, Date endDate) {
		long different = endDate.getTime() - startDate.getTime();
		return different;
	}

	public void writeInCSV(int threadIndex, String serviceName, Date startTime, Date endTime, long elapsedTime, int httpCode) {
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
			CSVUtils.writeLine(fw, Arrays.asList(serviceName, Integer.toString(threadIndex), sdf.format(startTime), sdf.format(endTime), Long.toString(elapsedTime), Integer.toString(httpCode)));
		} catch (IOException e1) {
			logErrorInBothLog("Can't write in the CSV file", e1);
		}
	}

	public void logErrorInBothLog(String errorMessage, Exception e) {
		logger.error(errorMessage, e);
		logWrap.aLogger.severe(threadName + " - " + errorMessage + " - " + e.getMessage());
	}

}
