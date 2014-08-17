package vn.com.web.vnds.concurrency;

import redis.clients.jedis.JedisPool;

public class JedisLockFactory {

	private TimeoutTracker timeoutTracker;

	public JedisLockFactory(TimeoutTracker timeoutTracker) {
		this.timeoutTracker = timeoutTracker;
	}

	public JedisLock produceJedisLock(String key, String identity, JedisPool pool) {
		return new JedisLock(timeoutTracker, key, identity, pool);
	}
}
