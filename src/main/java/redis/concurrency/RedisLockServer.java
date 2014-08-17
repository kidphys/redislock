package redis.concurrency;

import java.util.Timer;
import java.util.TimerTask;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class RedisLockServer implements LockServer {

	private static long EXPIRE_TIME = 5000;
	private static long PING_DURATION = 60000;
	private JedisPool pool;
	private Boolean isAlive = false;

	public RedisLockServer(JedisPool pool) {
		this.pool = pool;
		isAlive = ping();
	}

	private Boolean ping() {
		Jedis jedis = null;
		try {
			jedis = pool.getResource();
			return "PONG".equals(jedis.ping());
		} catch (JedisConnectionException e) {
			if (pool != null) {
				pool.returnBrokenResource(jedis);
			}
			return false;
		}
	}

	private void pollServer() {
		final Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				isAlive = ping();
			}
		}, 100, PING_DURATION);
	}

	public Boolean acquireLock(String key, String password) {
		Jedis jedis = null;
		try {
			jedis = pool.getResource();
			String status = jedis.set(key, password, "NX", "PX", EXPIRE_TIME);
			return "OK".equals(status);
		} catch (JedisConnectionException e) {
			isAlive = false;
			if (jedis != null) {
				pool.returnBrokenResource(jedis);
				pollServer();
			}
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

	public Boolean releaseLock(String key, String password) {
		Jedis jedis = null;
		try {
			jedis = pool.getResource();
			jedis.eval(buildDeleteScript(), 1, key, password);
			return true;
		} catch (JedisConnectionException e) {
			if (jedis != null) {
				pool.returnBrokenResource(jedis);
			}
			return false;
		} finally {
			if (jedis != null) {
				pool.returnResource(jedis);
			}
		}
	}

	/**
	 * We this check to be fast, hence the actually checking is handle by other thread
	 */
	public Boolean isAlive() {
		return isAlive;
	}
}
