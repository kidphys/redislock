package concurrent.lock;

import junit.framework.Assert;
import junit.framework.TestCase;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class LockTest extends TestCase {
	
	private final JedisPool jedisPool = new JedisPool("localhost", 6379);
	
	public void tearDown(){
		getJedis().del("foo", "bar");
		jedisPool.destroy();
	}
	
	public Jedis getJedis(){
		return jedisPool.getResource();
	}

	public void testSanity(){
		Assert.assertEquals(true, true);
	}

	public void testJedisSanity(){
		getJedis().set("foo", "bar");
		Assert.assertEquals("bar", getJedis().get("foo"));
	}

	public void testJedisSetNXSuccessFirstTime(){
		long result = getJedis().setnx("foo", "bar2");
		Assert.assertEquals(1, result);
		Assert.assertEquals("bar2", getJedis().get("foo"));
	}
	
	public void testJedisSetNXUnsuccess(){
		getJedis().set("foo", "bar");
		long result = getJedis().setnx("foo", "bar2");
		Assert.assertEquals(0, result);
		Assert.assertEquals("bar", getJedis().get("foo"));
	}
	
	public void testJedisSetSetNXMultipleTime(){
		getJedis().set("foo", "bar");
		long result = getJedis().setnx("foo", "bar2");
		Assert.assertEquals(0, result);
		Assert.assertEquals("bar", getJedis().get("foo"));
	}
	
	public void testTwoThreadCompeteForLock() throws InterruptedException{
		Thread t1 = new Thread(new Runnable() {
			public void run() {
				jedisPool.getResource().setnx("foo", "bar1");
			}
		});
		
		Thread t2 = new Thread(new Runnable() {
			public void run() {
				jedisPool.getResource().setnx("foo", "bar2");
			}
		});
		
		t1.start();
		t1.join();
		t2.start();
		t2.join();
		Assert.assertEquals("bar1", jedisPool.getResource().get("foo"));
	}
	
	public void testSetExpireDirectly() throws InterruptedException{
		Jedis jedis = getJedis();
		jedis.set("foo", "bar", "NX", "PX", 200);
		Assert.assertEquals("bar", jedis.get("foo"));
		Thread.sleep(300);
		Assert.assertEquals(null, jedis.get("foo"));
	}
	
	public String buildDeleteScript(){
		return "if redis.call(\"get\",KEYS[1]) == ARGV[1] \n"
				+ "then \n"
				+ "return redis.call(\"del\",KEYS[1]) \n" 
				+ "else return 0 \n" + 
				"end";
	}
	
	public void testCantDeleteKeyThatHasTheSameValue(){
		Jedis jedis = getJedis();
		String key = "foo", value = "bar";
		jedis.set(key, value);
		jedis.eval(buildDeleteScript(), 1, key, value);
		Assert.assertEquals(null, jedis.get(key));
	}

	public void testCanDeleteKeyThatHasTheDiffValue(){
		Jedis jedis = getJedis();
		String key = "foo", value = "bar";
		jedis.set(key, value);
		jedis.eval(buildDeleteScript(), 1, key, value + "1"); // make it different
		Assert.assertEquals(value, jedis.get(key));
	}
}
