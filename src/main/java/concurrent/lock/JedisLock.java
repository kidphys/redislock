package concurrent.lock;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class JedisLock {
	private final int MAX_RETRY_ATTEMPT = 5;
	private String key;
	private String identity;
	private JedisPool pool;
	private long expireTime = 2000;
	private String lastHoldValue = getCurrentTimestamp();

	public JedisLock(String name, String identity, JedisPool pool) {
		this.key = name;
		this.identity = identity;
		this.pool = pool;
	}

	private void validateTimeUnit(TimeUnit unit) {
		if (unit != TimeUnit.MILLISECONDS && unit != TimeUnit.SECONDS) {
			throw new IllegalArgumentException(
					"Only accept MILLISECONDS or SECONDS");
		}
	}

	public void setExpireTime(long time, TimeUnit unit) {
		validateTimeUnit(unit);
		if (unit == TimeUnit.MILLISECONDS) {
			expireTime = time;
		} else if (unit == TimeUnit.SECONDS) {
			expireTime = time * 1000;
		}
	}

	private String getCurrentTimestamp() {
		Timestamp time = new Timestamp(Calendar.getInstance().getTimeInMillis());
		return time.toString();
	}

	public Boolean tryLock(long time, TimeUnit unit)
			throws InterruptedException {
		validateTimeUnit(unit);

		long timeBlock = time * 1000 / MAX_RETRY_ATTEMPT; // # attempt
		if (unit == TimeUnit.MILLISECONDS) {
			timeBlock = timeBlock > 0 ? timeBlock / 1000 : 10;
		} else if (unit == TimeUnit.SECONDS) {
			timeBlock = timeBlock > 0 ? timeBlock : 10;
		}

		for (int i = 0; i < MAX_RETRY_ATTEMPT; i++) {
			if (!tryLock()) {
				Thread.sleep(timeBlock);
			} else {
				return true;
			}
		}
		return false;
	}

	private static long lastFailedTime;

	private static synchronized void updateLastFailedTime(){
		if(lastFailedTime == 0){
			lastFailedTime = Calendar.getInstance().getTimeInMillis();
		}
	}
	
	public Boolean isTimeout(){
		if(lastFailedTime > 0 && Calendar.getInstance().getTimeInMillis() - lastFailedTime < 10 * 1000){
			return true;
		}
		return false;
	}
	
	/**
	 * Try lock with value
	 */
	public Boolean tryLock() {
		if(isTimeout()){
			return true;
		}
		Jedis jedis = null;
		try {
			jedis = pool.getResource();
			String tempLastHoldValue = identity + getCurrentTimestamp();
			String result = jedis.set(key, tempLastHoldValue, "NX", "PX",
					expireTime);
			if ("OK".equals(result)) {
				lastHoldValue = tempLastHoldValue;
				return true;
			}
			return false;
		} catch (JedisConnectionException e) {
			if(jedis != null){
				pool.returnBrokenResource(jedis);
			}
			updateLastFailedTime();
			throw e;
		} finally {
			if(jedis != null){
				pool.returnResource(jedis);
			}
		}
	}

	private static final String deleteScript = "if redis.call(\"get\",KEYS[1]) == ARGV[1] \n"
			+ "then \n"
			+ "return redis.call(\"del\",KEYS[1]) \n"
			+ "else return 0 \n" + "end";

	private String buildDeleteScript() {
		return deleteScript;
	}

	public void unlock() {
		if(isTimeout()){
			return;
		}
		
		Jedis jedis = null;
		try {
			jedis = pool.getResource();
			jedis.eval(buildDeleteScript(), 1, key, lastHoldValue);
		} catch (JedisConnectionException e) {
			if(jedis != null){
				pool.returnBrokenResource(jedis);
			}
			updateLastFailedTime();
			throw e;
		} finally {
			if(jedis != null){
				pool.returnResource(jedis);
			}
		}
	}

}
