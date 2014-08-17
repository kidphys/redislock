package redis.concurrency;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ITOneLayerWithWaitTime {
	TransactionScheduler threadDelay;

	@Before
	public void setUp(){
		threadDelay = new TransactionScheduler();
	}

	private void slowExecute(SynchronizedTransaction trans, final Counter counter){
		trans.execute(new TransactionExecutable() {
			public void fail() {
				counter.fail();
			}
			
			public void execute() {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				counter.success();
			}
		});
	}
	
	@Test
	public void testTrasactionCanBeExecutedWithWaitTime() throws InterruptedException {
		LocalLock lock = new LocalLock();
		final SynchronizedTransaction transaction = new SynchronizedTransaction(100L, TimeUnit.MILLISECONDS, lock);
		final Counter counter = new Counter();
		Thread t1 = new Thread(new Runnable() {
			public void run() {
				slowExecute(transaction, counter);
			}
		});
		Thread t2 = new Thread(new Runnable() {
			public void run() {
				slowExecute(transaction, counter);
			}
		});
		t1.start();
		t2.start();
		t1.join();
		t2.join();
		Assert.assertEquals(2, counter.getSuccessCount());
	}

}
