package concurrent.lock;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class TestJedisPerformance extends TestCase {

	private JedisPool pool;

	public void setUp() {
		pool = new JedisPool("localhost", 6379);
	}

	public void tearDown() {
		pool.destroy();
	}

	public void testSetPerformance() {
		long elapsedTime = executeTime(new Runnable() {
			public void run() {
				Jedis jedis;
				for (int i = 0; i < 3000; i++) {
					jedis = pool.getResource();
					jedis.set("key", String.valueOf(Math.random()));
					pool.returnResource(jedis);
				}

			}
		});
		Assert.assertTrue("Time exceed: " + elapsedTime, elapsedTime < 1100);
	}

	private long executeTime(Runnable runnable) {
		long start = Calendar.getInstance().getTimeInMillis();
		runnable.run();
		return Calendar.getInstance().getTimeInMillis() - start;
	}

	public void testPerformanceWithSameLock() {
		final JedisLock lock = new JedisLock("Name", "Identity", pool);
		long elapsedTime = executeTime(new Runnable() {
			public void run() {
				for (int i = 0; i < 3000; i++) {
					lock.tryLock();
					lock.unlock();
				}
			}
		});
		Assert.assertTrue("Time exceed: " + elapsedTime, elapsedTime < 1100);
	}

	public void testPerformanceWithDifferentLocks() {
		final List<JedisLock> list = new ArrayList<JedisLock>();
		for (int i = 0; i < 3000; i++) {
			list.add(new JedisLock("Name" + Integer.toString(i), "Name"
					+ Integer.toString(i), pool));
		}

		long elapsedTime = executeTime(new Runnable() {
			public void run() {
				for (JedisLock jedisLock : list) {
					jedisLock.tryLock();
					jedisLock.unlock();
				}
			}
		});
		Assert.assertTrue("Time exceed: " + elapsedTime, elapsedTime < 1100);
	}

	private Thread createLockUnlockThread(final JedisLock lock) {
		return new Thread(new Runnable() {
			public void run() {
				// TODO Auto-generated method stub
				for (int i = 0; i < 2000; i++) {
					lock.tryLock();
					lock.unlock();
				}
			}
		});
	}

	public void testPerformanceMultipleThread() throws InterruptedException{
		long startTime = Calendar.getInstance().getTimeInMillis();
		final List<JedisLock> list = new ArrayList<JedisLock>();
		int COUNT = 5;
		for(int i = 0; i < COUNT; i++){
			list.add(new JedisLock("name" + String.valueOf(i), "identity", pool));
		}
		
		List<Thread> threadList = new ArrayList<Thread>();
		for(int i = 0; i < COUNT; i++){
			threadList.add(createLockUnlockThread(list.get(i)));
		}
		for(Thread t : threadList){
			t.start();
		}
		for(Thread t : threadList){
			t.join();
		}
		long elapsedTime = Calendar.getInstance().getTimeInMillis() - startTime;
		Assert.assertTrue("Time exceed: " + elapsedTime, elapsedTime < 1100);
	}
}
