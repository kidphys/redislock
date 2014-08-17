package concurrent.lock;

import redis.clients.jedis.JedisPool;
import junit.framework.Assert;
import junit.framework.TestCase;

public class TestJedisLockTimeout extends TestCase {

	private JedisLock lock;
	
	public void setUp(){
		JedisPool pool = new JedisPool("dummyhost", 6379);
		lock = new JedisLock("name", "identity", pool);
	}
	
	public void testAfterFailNextTryLockAlwaysTrue(){
		try{
			lock.tryLock();
		}
		catch(RuntimeException ex){
			
		}
		finally{
			Assert.assertTrue(lock.tryLock());
		}
	}
	
}
