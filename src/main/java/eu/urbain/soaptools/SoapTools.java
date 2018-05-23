package eu.urbain.soaptools;

import java.io.InputStream;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SoapTools {

	final static Logger logger = Logger.getLogger(SoapTools.class);

	public static void main(String[] args) {
		
		long start = System.currentTimeMillis();
		logger.info("Start SoapTool");

		Properties prop = new Properties();
		InputStream input = null;
		int rampup;
		String tmp;

		try {
			SoapTools obj = new SoapTools();
			input = obj.readFile("config.properties");

			// load a properties file
			prop.load(input);
			logger.info("Properties read:");
			Map<String, String> sortedMap = new TreeMap(prop);

			// output sorted properties (key=value)
			for (String key : sortedMap.keySet()) {
				logger.debug(key + "=" + sortedMap.get(key));
			}

			tmp = prop.getProperty("rampup");
			rampup = Integer.parseInt(tmp);

			if (rampup < 1) {
				throw new Exception("Not a valid rampup");
			}

			int serviceIndex = 0;
			String serviceName;
			String serviceUrl;
			String serviceRequestXmlPath;
			int serviceWait;
			int serviceRepetition;
			int serviceThreads;
			int index = 0;

			while (tmp != null) {
				logger.debug("");

				serviceName = prop.getProperty("service." + serviceIndex + ".name");
				logger.debug("Name service " + index  + " : " + serviceName);

				serviceUrl = prop.getProperty("service." + serviceIndex + ".url");
				logger.debug("URL : " + serviceUrl);

				serviceRequestXmlPath = prop.getProperty("service." + serviceIndex + ".request");
				logger.debug("Request : " + serviceRequestXmlPath);

				tmp = prop.getProperty("service." + serviceIndex + ".repetition");
				serviceRepetition = Integer.parseInt(tmp);
				logger.debug("Number of repetition : " + serviceRepetition);
				
				tmp = prop.getProperty("service." + serviceIndex + ".wait");
				serviceWait = Integer.parseInt(tmp);
				logger.debug("Delay between repetition : " + serviceWait);

				tmp = prop.getProperty("service." + serviceIndex + ".threads");
				serviceThreads = Integer.parseInt(tmp);
				logger.debug("Number of thread : " + serviceThreads);

				String threadName;

				for (int i = 0; i < serviceThreads; i++) {
					threadName = serviceName + "[" + i + "]";
					SoapThread t = new SoapThread(threadName, serviceName, serviceUrl, serviceRequestXmlPath, serviceWait, serviceRepetition);

					Thread t1 = new Thread(t);
					t1.start();

					logger.debug("Will sleep " + rampup + "ms before start next thread " + new Date());
					Thread.sleep(rampup);
					logger.debug("Sleep over, can start a new thread " + new Date());
				}

				serviceIndex++;
				tmp = prop.getProperty("service." + serviceIndex + ".name");
			}
		} catch (Exception ex) {
			logger.error("General Exception", ex);
		}
		long elapsedTime = System.currentTimeMillis() - start;
		logger.info("End SoapTool : " + elapsedTime + "ms");
	}

	private InputStream readFile(String path) {
		InputStream returnInput;
		ClassLoader classLoader = getClass().getClassLoader();
		returnInput = classLoader.getResourceAsStream(path);
		return returnInput;
	}

}
