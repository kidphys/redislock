package concurrent.lock;

import java.util.Calendar;

import junit.framework.Assert;
import junit.framework.TestCase;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class TestPerformanceJedisLock extends TestCase {

	private JedisPool pool;

	public void setUp(){
		JedisPoolConfig config = new JedisPoolConfig();
//		config.setBlockWhenExhausted(false);
		config.setMaxWaitMillis(200);
		config.setMaxTotal(100);
		pool = new JedisPool(config, "localhost", 6379);
	}
	public void tearDown(){
		pool.destroy();
	}

	public void testAlotOfLockUnlock(){
		long start = Calendar.getInstance().getTimeInMillis();
		JedisLock lock = new JedisLock("key", "value", pool);
		for(int i = 0; i < 3000; i++){
			lock.tryLock();
			lock.unlock();
		}
		long elapsedTime = Calendar.getInstance().getTimeInMillis() - start;
		Assert.assertTrue("Time exceed: " + elapsedTime, elapsedTime < 1000);
	}
}
