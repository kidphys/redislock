package redis.concurrency;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import redis.clients.jedis.JedisPool;

/**
 * Different kinds of lock (local or redis) should produce the same effect i.e. same test scenarios
 * This abstract test provide base scenarios for all tests
 */
@RunWith(Parameterized.class)
public class ITSynchronizedTransaction{
SynchronizedTransaction executor;
	TransactionScheduler scheduler;

	public ITSynchronizedTransaction(SynchronizedTransaction executor) {
		this.executor = executor;
		scheduler = new TransactionScheduler();
	}
	
	private static TransactionLock twoLayerSafeLocalLock(){
		return new TwoLayerLock(new LocalLock(), new LocalLock());
	}
	
	private static DistributedLock createRedisLock(String hostAddr, int port){
		JedisPool pool = new JedisPool(hostAddr, port);
		RedisLockServer server = new RedisLockServer(pool);
		return new DistributedLock(server, "foo");
	}
	
	private static TransactionLock deadInnerRedisLock(){
		return new TwoLayerLock(new LocalLock(), createRedisLock("dead-server", 1234));
	}

	private static TransactionLock liveRedisLock(){
		RedisEnvironment env = new RedisEnvironment();
		return createRedisLock(env.getHost(), env.getPort());
	}
	
	@Parameterized.Parameters
	public static Iterable<Object[]> createExecutors() throws InterruptedException{
		return Arrays.asList(new Object[][]{
			{new SynchronizedTransaction(new LocalLock())},
			{new SynchronizedTransaction(twoLayerSafeLocalLock())},
			{new SynchronizedTransaction(deadInnerRedisLock())},
			{new SynchronizedTransaction(liveRedisLock())}
		});
	}
	
	
	@Before
	public void setUp() {
		scheduler = new TransactionScheduler();
		scheduler.resetCounter();
	}

	@Test
	public void testAnonymousSanity() throws InterruptedException {
		executor.execute(scheduler.createBlockingTask(0));
		Assert.assertEquals(1, scheduler.getCompletedTransaction());
	}

	@Test
	public void lateThreadCantAcquireLockIfItWaitNotLongEnough() throws InterruptedException {
		executor.setWaitTime(50);
		Thread t1 = scheduler.scheduleBlockingTask(executor, 0, 100);
		Thread t2 =  scheduler.scheduleBlockingTask(executor, 10, 100);
		t1.start();
		t2.start();
		t1.join();
		t2.join();
		Assert.assertEquals(1, scheduler.getFailedTransaction());
		Assert.assertEquals(1, scheduler.getCompletedTransaction());
	}

	@Test
	public void test2ThreadTryToExecuteAfterEachOther() throws InterruptedException {
		Thread t1 = scheduler.scheduleBlockingTask(executor, 0, 0);
		Thread t2 = scheduler.scheduleBlockingTask(executor, 100, 0);
		t1.start();
		t2.start();
		t1.join();
		t2.join();
		Assert.assertEquals(2, scheduler.getCompletedTransaction());
	}

	@Test
	public void testLockMustReleaseAfterExecuteThrowException() throws InterruptedException {
		Thread t1 = scheduler.createGenericExecutorThread(executor, scheduler.createExceptionExecutable());
		Thread t2 = scheduler.scheduleBlockingTask(executor, 100, 0);
		t1.start();
		t2.start();
		t1.join();
		t2.join();
		Assert.assertEquals(2, scheduler.getCompletedTransaction());
	}

	@Test
	public void testFirstOneInKickOutTheRest() throws InterruptedException {
		executor.setWaitTime(10);
		List<Thread> list = new ArrayList<Thread>();
		for (int i = 0; i < 5; i++) {
			list.add(scheduler.scheduleBlockingTask(executor, 0, 100));
		}

		for(Thread th : list){
			th.start();
		}
		for(Thread th : list){
			th.join();
		}
		Assert.assertEquals(1, scheduler.getCompletedTransaction());
	}


}
