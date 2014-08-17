package redis.concurrency;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Sometimes we'll want to timeout all jedis lock in the system if Redis server fails
 * Use this factory to bind all jedis lock to 1 timeout tracker
 * @author chau.hoang
 *
 */
public class JedisLockFactory {
	private static int MAX_TOTAL = 20;
	private static int MAX_WAIT_MILLIS = 2000;

	private TimeoutTracker timeoutTracker;

	public JedisLockFactory(TimeoutTracker timeoutTracker) {
		this.timeoutTracker = timeoutTracker;
	}

	public JedisLock produceJedisLock(String key, String identity, JedisPool pool) {
		return new JedisLock(timeoutTracker, key, pool);
	}
	
	public static JedisPoolConfig createDefaultPool(){
		JedisPoolConfig config = new JedisPoolConfig();
		config.setMaxTotal(MAX_TOTAL);
		config.setMaxWaitMillis(MAX_WAIT_MILLIS);
		return config;
	}
}
