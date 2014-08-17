package config;


import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GetPropertiesValueTest {
	
	private final String configFilePath = "test-config.properties";

	private InputStream inputStream;
	private Properties properties;

	@Before
	public void setUp() {
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		inputStream = loader.getResourceAsStream(configFilePath);
		properties = new Properties();
	}

	@Test
	public void testGetPropertiesValue() throws IOException {
		properties.load(inputStream);
		Assert.assertTrue(StringUtils.isNotEmpty(properties.getProperty("redis.server.name")));
	}

	@After
	public void tearDown() throws IOException {
		if (inputStream != null) {
			inputStream.close();
		}
	}

}
