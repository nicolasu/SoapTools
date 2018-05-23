package eu.urbain.soaptools;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

public class SoapThread implements Runnable {

	private final String threadName;

	private final String serviceName;
	private final String serviceUrl;
	private final String serviceRequestXmlPath;
	private final int serviceWait;
	private final int serviceRepetition;

	private Logger logger;
	FileAppender appender;

	SoapThread(String threadName_, String serviceName_, String serviceUrl_, String serviceRequestXmlPath_, int serviceWait_, int serviceRepetition_) {
		serviceName = serviceName_;
		serviceUrl = serviceUrl_;
		serviceRequestXmlPath = serviceRequestXmlPath_;
		serviceWait = serviceWait_;
		serviceRepetition = serviceRepetition_;
		threadName = threadName_;

		appender = new FileAppender();
		appender.setFile("logs/" + threadName + ".log");
		appender.setLayout(new PatternLayout("%d [%t] %-5p %c - %m%n"));
		appender.activateOptions();
		logger = Logger.getLogger(threadName);
		logger.setAdditivity(false);
		logger.addAppender(appender);
		logger.info("[Constructor] - Welcome in the log of thread " + threadName);

		// logger.info("Constructeur SoapThread " + threadName + " - " + serviceName + "
		// - " + serviceUrl + " - " + serviceRequestXmlPath + " - " + serviceWait
		// + " - " + serviceRepetition);

	}

	@Override
	public void run() {
		logger.info("Start SoapThread with parameter [" + threadName + "] [" + serviceName + "] [" + serviceUrl + "] [" + serviceRequestXmlPath + "] ["
				+ serviceWait + "] [" + serviceRepetition + "]");

		String requestString = readFile(serviceRequestXmlPath);
		logger.info(requestString);
		URL u;

		for (int iLoop = 1; iLoop <= serviceRepetition; iLoop++) {
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

				long start = System.currentTimeMillis();
				logger.info("Send request " + iLoop + "/" + serviceRepetition);
				InputStream in = connection.getInputStream();
				long elapsedTime = System.currentTimeMillis() - start;
				logger.info("Response " + iLoop + "/" + serviceRepetition + " - time: " + elapsedTime + "ms");
				String text = IOUtils.toString(in, StandardCharsets.UTF_8.name());
				logger.info(text);

				in.close();
				connection.disconnect();

			} catch (MalformedURLException e) {
				logger.error("Can't call the webservice, bad URL", e);
			} catch (IOException e) {
				logger.error("Can't call the webservice, can't openConnection", e);
			}

			try {
				Thread.sleep(serviceWait);
			} catch (InterruptedException e) {
				logger.error("Can't sleep the thread", e);
			}

		}

		// Remove appender
		logger.removeAppender(appender);

	}

	private String readFile(String path) {
		logger.info("Try read file " + serviceRequestXmlPath);
		String result = "";
		ClassLoader classLoader = getClass().getClassLoader();
		//logger.info(classLoader);
		try {
			InputStream is = classLoader.getResourceAsStream(serviceRequestXmlPath);
			//logger.info(is);
			result = IOUtils.toString(is, "UTF-8");
			//logger.info(result);
		} catch (IOException e) {
			logger.error("Can't read file " + serviceRequestXmlPath, e);
		}
		return result;
	}
	


}
