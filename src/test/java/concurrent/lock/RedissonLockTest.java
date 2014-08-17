package concurrent.lock;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.redisson.Redisson;
import org.redisson.core.RLock;

public class RedissonLockTest extends TestCase {

	public void testSanity() {
		Redisson redisson = Redisson.create();
		RLock lock = redisson.getLock("SomeLock");
		lock.lock();
		lock.unlock();
		redisson.shutdown();
	}
	
	public void testUnlock(){
		Redisson redisson = Redisson.create();
		RLock lock = redisson.getLock("SomeLock");
		lock.lock();
		Assert.assertEquals(false, lock.tryLock());
	}

//	public void test2Thread() throws InterruptedException{
//		final Redisson redisson = Redisson.create();
//		Thread t1 = new Thread(new Runnable() {
//			
//			public void run() {
//				RLock lock = redisson.getLock("SomeLock");
//				try{
//                if(lock.tryLock()){
//                        lock.lock();
//                }
//				}
//				finally{
//					lock.unlock();
//				}
//			}
//		});
//		t1.start();
//		t1.join();
//		Assert.assertEquals(true, redisson.getLock("SomeLock").tryLock());
//	}
}
