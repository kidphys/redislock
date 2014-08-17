package redis.concurrency;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisConnectionException;

interface JedisExecutable {
	void execute(Jedis jedis);
}

public class ITJedis {
	private JedisPool pool;

	@Before
	public void setUp() throws Exception {
		RedisEnvironment env = new RedisEnvironment();
		pool = new JedisPool(env.getHost(), env.getPort());
	}

	@After
	public void tearDown() throws Exception {
		pool.destroy();
	}

	private void executeJedisPoolSafely(JedisExecutable executable) {
		Jedis jedis = null;
		try {
			jedis = pool.getResource();
			executable.execute(jedis);
		} catch (JedisConnectionException ex) {
			Assert.fail("Failed to connection to Redis server" + ex.getStackTrace());
			if (jedis != null) {
				pool.returnBrokenResource(jedis);
			}
		} finally {
			if (jedis != null) {
				pool.returnResource(jedis);
			}
		}
	}

	@Test
	public void testRedisServerConnection() {
		executeJedisPoolSafely(new JedisExecutable() {
			public void execute(Jedis jedis) {
				String value = String.valueOf(Math.random());
				jedis.set("key", value);
				Assert.assertEquals(value, jedis.get("key"));
				jedis.del("key");
			}
		});
	}

	@Test
	public void testRedisSetCommand(){
		executeJedisPoolSafely(new JedisExecutable() {
			public void execute(Jedis jedis) {
				String key = String.valueOf(Math.random());
				String result = jedis.set(key, "value", "NX", "PX", 100);
				Assert.assertEquals("OK", result);
				Assert.assertNotSame("OK", jedis.set(key, "value", "NX", "PX", 100));
				jedis.del(key);
			}
		});
	}
}
