package redis.concurrency;

import java.sql.Timestamp;

import java.util.Calendar;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class JedisLock implements TransactionLock {
	private final int MAX_RETRY_ATTEMPT = 5;
	private long expireTime = 5000;
	private String lastHoldValue = getTimestamp();

	private String key;
	private String identity;
	private JedisPool pool;
	private TimeoutTracker timeoutTracker;

	public JedisLock(TimeoutTracker timeoutTracker, String key, JedisPool pool) {
		this.timeoutTracker = timeoutTracker;
		this.key = key;
		this.identity = UUID.randomUUID().toString();
		this.pool = pool;
	}

	public void setExpireTime(long duration, TimeUnit unit) {
		expireTime = unit.toMillis(duration);
	}

	private String getTimestamp() {
		Timestamp time = new Timestamp(Calendar.getInstance().getTimeInMillis());
		return time.toString();
	}

	public Boolean tryLock(long duration, TimeUnit unit)
			throws InterruptedException {
		long sleepInterval = duration / MAX_RETRY_ATTEMPT;
		for (int i = 0; i < MAX_RETRY_ATTEMPT; i++) {
			if (!tryLock()) {
				unit.sleep(sleepInterval);
			} else {
				return true;
			}
		}
		return false;
	}

	private synchronized void updateLastFailedTime() {
		timeoutTracker.triggerFailedTime();
	}

	public Boolean isTimeout() {
		return timeoutTracker.shouldRetry();
	}

	/**
	 * Try lock with value
	 */
	public Boolean tryLock() {
		if (isTimeout()) {
			return true;
		}

		Jedis jedis = null;
		try {
			jedis = pool.getResource();
			String newHoldValue = identity + getTimestamp();
			String result = jedis.set(key, newHoldValue, "NX", "PX",
					expireTime);
			if ("OK".equals(result)) {
				lastHoldValue = newHoldValue;
				return true;
			}
			return false;
		} catch (JedisConnectionException e) {
			if (jedis != null) {
				pool.returnBrokenResource(jedis);
			}
			updateLastFailedTime();
			throw e;
		} finally {
			if (jedis != null) {
				pool.returnResource(jedis);
			}
		}
	}

	private String buildDeleteScript() {
		return "if redis.call(\"get\",KEYS[1]) == ARGV[1] \n" + "then \n"
				+ "return redis.call(\"del\",KEYS[1]) \n" + "else return 0 \n"
				+ "end";
	}

	public void unlock() {
		if (isTimeout()) {
			return;
		}

		Jedis jedis = null;
		try {
			jedis = pool.getResource();
			jedis.eval(buildDeleteScript(), 1, key, lastHoldValue);
		} catch (JedisConnectionException e) {
			if (jedis != null) {
				pool.returnBrokenResource(jedis);
			}
		} finally {
			if (jedis != null) {
				pool.returnResource(jedis);
			}
		}
	}
}
