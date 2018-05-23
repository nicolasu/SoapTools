
package be.axi.soaptool;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

	final static Logger logger = Logger.getLogger(Application.class);

	public static void main(String[] args) {

		logger.info("Start SoapTool");
		long start = System.currentTimeMillis();

		Properties prop = new Properties();
		InputStream input = null;
		int rampup;
		String tmp;

		try {

			input = new FileInputStream("src/main/resources/config.properties");

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

			while (tmp != null) {
				logger.debug("");

				serviceName = prop.getProperty("service." + serviceIndex + ".name");
				logger.debug(serviceName);

				serviceUrl = prop.getProperty("service." + serviceIndex + ".url");
				logger.debug(serviceUrl);

				serviceRequestXmlPath = prop.getProperty("service." + serviceIndex + ".request");
				logger.debug(serviceRequestXmlPath);

				tmp = prop.getProperty("service." + serviceIndex + ".wait");
				serviceWait = Integer.parseInt(tmp);
				logger.debug(serviceWait);

				tmp = prop.getProperty("service." + serviceIndex + ".repetition");
				serviceRepetition = Integer.parseInt(tmp);
				logger.debug(serviceRepetition);

				tmp = prop.getProperty("service." + serviceIndex + ".threads");
				serviceThreads = Integer.parseInt(tmp);
				logger.debug(serviceThreads);

				String threadName;

				for (int i = 0; i < serviceThreads; i++) {
					threadName = serviceName + "[" + i + "]";
					SoapThread t = new SoapThread(threadName, serviceName, serviceUrl, serviceRequestXmlPath, serviceWait, serviceRepetition);
					// t.run();

					Thread t1 = new Thread(t);
					t1.start();

					logger.debug("Before sleep for rampup " + rampup + " main app " + new Date());
					Thread.sleep(rampup);
					logger.debug("After sleep main app " + new Date());
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

}
