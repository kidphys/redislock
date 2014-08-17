package concurrent.lock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;
import junit.framework.TestCase;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

class Mock {
	private int count;

	public void increase() {
		count++;
	}

	public void decrease() {
		count--;
	}

	public int getCount() {
		return count;
	}
}

public class TestJedisLock extends TestCase {
	private JedisPool pool;
	private JedisLock lock;
	private static final String targetKey = "key";
	private static final String defaultIdentity = "myIdentity";
	private Mock success;
	private Mock fail;

	public void setUp() {
		JedisPoolConfig config = new JedisPoolConfig();
		config.setMaxTotal(2);
		config.setMaxWaitMillis(200);
		pool = new JedisPool(config, "localhost", 6379);
		Jedis jedis = pool.getResource();
		jedis.del(targetKey); // delete key no matter what to reset
		pool.returnResource(jedis);
		lock = new JedisLock(targetKey, defaultIdentity, pool);

		success = new Mock();
		fail = new Mock();
	}

	public void tearDown() {
//		pool.destroy();
	}

	public void testTryLockReturnTrueIfNoLock() {
		Assert.assertTrue(lock.tryLock());
	}

	public void testTryLockIsFalseTheSecondTime() {
		lock.tryLock();
		Assert.assertFalse(lock.tryLock());
	}

	public void testLockCanBeAcquireAfterUnlock() {
		lock.tryLock();
		lock.unlock();
		Assert.assertTrue(lock.tryLock());
	}

	public void testLockCanUnlockAfter2TryLock() {
		lock.tryLock();
		lock.tryLock();
		lock.unlock();
		Assert.assertTrue(lock.tryLock());
	}

	public void testLateLockCannotAcquireTheLock() {
		lock.tryLock();
		// even with the same identity!!
		JedisLock secondLock = new JedisLock(targetKey, defaultIdentity, pool);
		Assert.assertFalse(secondLock.tryLock());
	}

	public void testLateLockCannotUnlock() {
		lock.tryLock();
		// even with the same identity!!
		JedisLock secondLock = new JedisLock(targetKey, defaultIdentity, pool);
		secondLock.unlock();
		Assert.assertFalse(lock.tryLock());
	}

	public void testLateLockCanAcquireLockIfKeyExpire()
			throws InterruptedException {
		JedisLock lock1 = new JedisLock(targetKey, defaultIdentity, pool);
		JedisLock lock2 = new JedisLock(targetKey, defaultIdentity, pool);
		lock1.setExpireTime(200, TimeUnit.MILLISECONDS);
		lock1.tryLock();
		Thread.sleep(210);
		// now lock expired, lock2 can acquire it
		Assert.assertTrue(lock2.tryLock());
	}

	private Thread getLockingThread(final int milliseconds) {
		return new Thread(new Runnable() {

			public void run() {
				if (lock.tryLock()) {
					try {
						Thread.sleep(milliseconds);
						success.increase();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} finally {
						lock.unlock();
					}
				}
				else{
					fail.increase();
				}
			}
		});
	}

	public void testLockInOneThread() throws InterruptedException {
		Thread t = getLockingThread(200);
		t.start();
		t.join();
		Assert.assertTrue(lock.tryLock());
	}
	
	public void testFirstComerKickOutTheRest() throws InterruptedException{
		Thread t1 = getLockingThread(10000);
		List<Thread> list = new ArrayList<Thread>();
		for(int i = 0; i < 5; i++){
			list.add(getLockingThread(10));
		}
		t1.start();
		for(Thread th : list){
			th.start();
		}
		t1.join();
		for(Thread th : list){
			th.join();
		}
		Assert.assertEquals(1, success.getCount());
		Assert.assertEquals(5, fail.getCount());

	}

	public void test2LockCanGoWithTryTime() throws InterruptedException {
		final JedisLock lock = new JedisLock(targetKey + "!", defaultIdentity
				+ "!", pool);
		final Mock success = new Mock();
		final Mock fail = new Mock();
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					if (lock.tryLock()) {
						success.increase();
						Thread.sleep(10);
						lock.unlock();
					} else {
						fail.increase();
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (RuntimeException e) {
					e.printStackTrace();
				}
			}
		});

		Thread lateThread = new Thread(new Runnable() {
			public void run() {
				try {
					if (lock.tryLock(200, TimeUnit.MILLISECONDS)) {
						success.increase();
						lock.unlock();
					} else {
						fail.increase();
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (RuntimeException e) {
					e.printStackTrace();
				}
			}
		});
		t.start();
		lateThread.start();
		t.join();
		lateThread.join();
		Assert.assertEquals(0, fail.getCount());
		Assert.assertEquals(2, success.getCount());
	}
}
