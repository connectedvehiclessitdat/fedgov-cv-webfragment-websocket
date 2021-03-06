package gov.usdot.cv.websocket.server.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;

public class ConfigUtils {

	private static Logger logger = Logger.getLogger(ConfigUtils.class);
	private static final ObjectMapper mapper;
	
	static {
		mapper = new ObjectMapper();
	}
	
	public static <T> T loadConfigBean(String fileName, Class<T> genericType) throws ConfigurationException {
		InputStream is = ConfigUtils.getFileAsStream(fileName);
		if (is == null) {
			throw new ConfigurationException("Config file " + fileName + " not found");
		}
		T configBean = null;
		try {
			configBean = mapper.readValue(is, genericType);
		} catch (Exception e) {
			throw new ConfigurationException(e);
		}
		return configBean;
	}
	
	public static <T> List<T> loadConfigBeanList(String fileName, Class<T> genericType) throws ConfigurationException {
		InputStream is = ConfigUtils.getFileAsStream(fileName);
		if (is == null) {
			throw new ConfigurationException("Config file " + fileName + " not found");
		}
		List<T> configBeanList = null;
		try {
			configBeanList = mapper.readValue(is, 
				mapper.getTypeFactory().constructCollectionType(List.class, genericType));
		} catch (Exception e) {
			throw new ConfigurationException(e);
		}
		return configBeanList;
	}
	
	public static InputStream getFileAsStream(String fileName)  {
		InputStream is = null;
		File f = new File(fileName);
		logger.info("Attempting to find file " + fileName);
		if (f.exists()) {
			logger.debug("Loading file from the file system " + f.getAbsolutePath());
			try {
				is = new FileInputStream(f);
			} catch (FileNotFoundException e) {
				logger.warn(e);
			} finally {
				if ( is != null ) {
					try {
						is.close();
					} catch (IOException e) {
					}
				}
			}
		} else {
			logger.debug("File not found on file system, checking on classpath...");
			is = ConfigUtils.class.getClassLoader().getResourceAsStream(fileName);
		}
		if (is == null) {
			logger.error("File " + fileName + " could not be found");
		}
		return is;
	}
}
