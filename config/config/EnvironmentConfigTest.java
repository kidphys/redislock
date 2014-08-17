package config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.BeforeClass;

public class EnvironmentConfigTest {

	private static String configFilePath = "test-config.properties";

	public static Properties PROPERTIES;

	@BeforeClass
	public static void loadConfig() throws IOException {
		PROPERTIES = new Properties();
		InputStream resource = getResourceAsStream();
		if (resource != null) {
			PROPERTIES.load(resource);
			resource.close();
		}
	}

	private static InputStream getResourceAsStream() {
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		return loader.getResourceAsStream(configFilePath);
	}
}
