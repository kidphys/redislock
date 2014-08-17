package redis.concurrency;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class ITJedisLock {

	private static JedisPoolConfig createJedisConfig(){
		JedisPoolConfig config = new JedisPoolConfig();
		config.setMaxTotal(20);
		config.setMaxWaitMillis(2000);
		return config;
	}

	private static JedisPool pool;
	private static JedisLockFactory factory;
	private static Counter mock = new Counter();
	private JedisLock lock;
	private static RedisEnvironment env;


	@BeforeClass
	public static void beforeClass() throws IOException{
		env = new RedisEnvironment();
		pool = new JedisPool(createJedisConfig(), env.getHost(), env.getPort());
		factory = new JedisLockFactory(new TimeoutTracker());
	}

	@AfterClass
	public static void afterClass(){
		pool.destroy();
	}

	@Before
	public void before(){
		lock = factory.produceJedisLock(String.valueOf(Math.random()), "identity", pool);
	}

	private Thread getSleepLockThread(final JedisLock lock, final int duration){
		return new Thread(new Runnable() {
			public void run() {
				try {
					if(lock.tryLock(100, TimeUnit.MILLISECONDS)){
						mock.success();
					}
					Thread.sleep(duration);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				finally{
					lock.unlock();
				}
			}
		});
	}

	private Thread getSleepThreadAcquirePool(final int duration) {
		return new Thread(new Runnable() {
			public void run() {
				Jedis jedis = pool.getResource();
				try {
					Thread.sleep(duration);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				finally{
					pool.returnResource(jedis);
				}
			}
		});
	}

	@Test
	public void testLockUnlock() throws InterruptedException{
		JedisLock lock = factory.produceJedisLock("Test", "identity", pool);

		lock.tryLock(100, TimeUnit.MILLISECONDS);
		lock.unlock();
		Assert.assertTrue("After unlock, lock can be acquired again", lock.tryLock(100, TimeUnit.MILLISECONDS));
		lock.unlock();
	}

	@Test
	public void acquireALockTooLong() throws InterruptedException {
		Thread t1 = getSleepThreadAcquirePool(2000);
		Thread t2 = getSleepThreadAcquirePool(10);
		t1.start();
		t2.start();
		t1.join();
		t2.join();
	}

	@Test
	public void aquirePoolTwice() throws InterruptedException{
		Jedis j1 = pool.getResource();
		Thread.sleep(100);
		Jedis j2 = pool.getResource();
		pool.returnResource(j1);
		pool.returnResource(j2);
	}

	@Test
	public void testFirstComerLockOutTheRest() throws InterruptedException{
		lock.tryLock(100, TimeUnit.MILLISECONDS);
		for(int i = 0; i < 5; i++){
			Assert.assertFalse(lock.tryLock(100, TimeUnit.MILLISECONDS));
		}
		lock.unlock();
	}

	@Test
	public void firstThreadLockOutTheRest() throws InterruptedException{
		List<Thread> list = new ArrayList<Thread>();
		for(int i = 0; i < 5; i++){
			list.add(getSleepLockThread(lock, 200));
		}
		for(Thread th : list){
			th.start();
		}
		for(Thread th : list){
			th.join();
		}
		Assert.assertEquals("Redis host info: " + env.getHost()  + ":" + env.getPort(), 1, mock.getSuccessCount());
	}
}
